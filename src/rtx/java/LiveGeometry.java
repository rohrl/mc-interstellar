import io.github.rohrl.interstellar.client.WorldRenderBackend;
import io.github.rohrl.interstellar.client.RefreshProfile;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.*;
import java.util.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.*;

/** Resident compact terrain, independently replaceable chunk BLAS, bounded moving geometry.
 * All mutation follows queue completion; no buffers/structures are allocated on steady frames. */
final class LiveGeometry implements AutoCloseable {
    private static final boolean BATCH_UPLOAD=Boolean.parseBoolean(System.getProperty("interstellar.batchTerrainUpload","true"));
    private static final int ROW_BYTES=4092*16,MAX_MOVING=200_000;
    private final Probe vk;
    private final WorldRenderBackend.Terrain source;
    Probe.Buffer terrain;
    final Probe.Buffer moving;
    private Probe.Buffer indices,staging,instances;
    private int indexQuads,actorCount,cloudCount;
    private long terrainRevision=-1,movingRevision=-1;
    private final Map<Long,Chunk> chunks=new LinkedHashMap<>();
    private final Structure actors=new Structure(false),clouds=new Structure(false);
    final Structure top=new Structure(true);
    private record Chunk(WorldRenderBackend.Chunk metadata,Structure structure) {}
    private boolean changed=true;
    private long updateCount;
    float[] checkRay;
    int checkVertex;

    LiveGeometry(Probe vk,WorldRenderBackend.Terrain source) {
        this.vk=vk;this.source=source;source.retainUpdates(true);
        try {
        if((long)source.rows()*ROW_BYTES>vk.maxStorageBufferRange)throw new IllegalStateException("GPU storage-buffer range cannot address the retained terrain arena");
        terrain=terrainBuffer((long)source.rows()*ROW_BYTES);
        moving=vk.buffer(MAX_MOVING*144L,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT|VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR,true);
        staging=vk.buffer(16*1024*1024,VK_BUFFER_USAGE_TRANSFER_SRC_BIT,true);
        instances=vk.buffer(2048*64,VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR,true);
        synchronizeTerrain();
        } catch(RuntimeException|LinkageError failure){close();throw failure;}
    }
    void update(float[] data,int count,long revision) {
        synchronizeTerrain();
        if(revision==movingRevision)return;
        if(count<0 || count>MAX_MOVING)throw new IllegalArgumentException("Moving geometry capacity exceeded");
        actorCount=0;cloudCount=0;
        for(int i=0;i<count;i++){float tag=data[i*36+3];if(tag==5 || tag==6)cloudCount++;else actorCount++;}
        var floats=moving.mapped().asFloatBuffer();int actor=0,cloud=actorCount;
        for(int i=0;i<count;i++){float tag=data[i*36+3];int to=(tag==5 || tag==6)?cloud++:actor++;floats.position(to*36);floats.put(data,i*36,36);}
        actors.ensure(actorCount,0,0);clouds.ensure(cloudCount,0,0);
        int at=0;
        for(var chunk:chunks.values())instance(at++,chunk.structure,chunk.metadata.row()*341,1);
        instance(at++,actors,0x800000,actorCount>0?2:0);
        instance(at++,clouds,0x800000|actorCount,cloudCount>0?4:0);
        top.ensure(at,0,0);movingRevision=revision;changed=true;
    }
    private void instance(int at,Structure structure,int index,int mask) {
        if((at+1)*64L>instances.size())throw new IllegalStateException("RTX instance capacity exceeded");
        var row=VkAccelerationStructureInstanceKHR.create(MemoryUtil.memAddress(instances.mapped())+at*64L);
        for(int i=0;i<12;i++)row.transform().matrix(i,i==0||i==5||i==10?1:0);
        row.instanceCustomIndex(index).mask(mask).instanceShaderBindingTableRecordOffset(0).flags(VK_GEOMETRY_INSTANCE_TRIANGLE_FACING_CULL_DISABLE_BIT_KHR).accelerationStructureReference(structure.address);
    }
    private void synchronizeTerrain() {
        if(terrainRevision==source.revision())return;
        long capacity=(long)source.rows()*ROW_BYTES;
        if(capacity>terrain.size()) {
            var replacement=terrainBuffer(capacity);long started=System.nanoTime();
            try(MemoryStack s=MemoryStack.stackPush()) {
                vk.begin();vkCmdCopyBuffer(vk.command,terrain.handle(),replacement.handle(),VkBufferCopy.calloc(1,s).size(terrain.size()));
                barrier(VK_ACCESS_TRANSFER_WRITE_BIT,VK_ACCESS_SHADER_READ_BIT|VK_ACCESS_ACCELERATION_STRUCTURE_READ_BIT_KHR);vk.finish();
            } catch(RuntimeException e){vk.release(replacement);throw e;}
            // Completed BLAS builds own their geometry: input vertex buffers are not retained.
            vk.release(terrain);terrain=replacement;
            System.out.println("RTX terrain storage grown: bytes="+capacity+" copyMs="+(System.nanoTime()-started)/1e6+" retainedChunks="+chunks.size());
        }
        long started=System.nanoTime();
        var next=source.chunks();var keys=new HashSet<Long>();for(var chunk:next)keys.add(chunk.key());
        for(var iterator=chunks.entrySet().iterator();iterator.hasNext();) {var old=iterator.next();if(!keys.contains(old.getKey())){old.getValue().structure.close();iterator.remove();}}
        int builds=0;
        for(var metadata:next) {
            var old=chunks.get(metadata.key());if(old!=null && old.metadata.equals(metadata))continue;
            ensureIndices(metadata.quads());
            long stage=RefreshProfile.start();var bytes=source.read(metadata);
            RefreshProfile.end(RefreshProfile.READ,stage);
            stage=RefreshProfile.start();
            boolean combined=RefreshProfile.experiment("batch",BATCH_UPLOAD) && (long)metadata.quads()*192<=staging.size();
            try {
                if(checkRay==null)for(int q=0;q<metadata.quads();q++) {
                    int p=q*192;float[] a=new float[3],u=new float[3],v=new float[3];
                    for(int k=0;k<3;k++){a[k]=bytes.getFloat(p+k*4);u[k]=bytes.getFloat(p+48+k*4)-a[k];v[k]=bytes.getFloat(p+96+k*4)-a[k];}
                    float[] n={u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]};float length=(float)Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);
                    if(length<.01f)continue;checkRay=new float[6];for(int k=0;k<3;k++){n[k]/=length;checkRay[k]=a[k]+(u[k]+v[k])/3+n[k]*.125f;checkRay[k+3]=-n[k]*.25f;}checkVertex=metadata.row()*4092+q*12;
                    System.out.println("RTX check ray="+Arrays.toString(checkRay)+" vertex="+Arrays.toString(a)+" offset="+checkVertex);break;
                }
                int length=metadata.quads()*192;
                for(int offset=0;offset<length;) {
                    int size=(int)Math.min(length-offset,staging.size());
                    MemoryUtil.memCopy(MemoryUtil.memAddress(bytes)+offset,MemoryUtil.memAddress(staging.mapped()),size);
                    try(MemoryStack s=MemoryStack.stackPush()) {
                        vk.begin();vkCmdCopyBuffer(vk.command,staging.handle(),terrain.handle(),VkBufferCopy.calloc(1,s).srcOffset(0).dstOffset((long)metadata.row()*ROW_BYTES+offset).size(size));
                        barrier(VK_ACCESS_TRANSFER_WRITE_BIT,VK_ACCESS_ACCELERATION_STRUCTURE_READ_BIT_KHR|VK_ACCESS_SHADER_READ_BIT);if(!combined)vk.finish();
                    }
                    offset+=size;
                }
            } finally {MemoryUtil.memFree(bytes);}
            RefreshProfile.end(RefreshProfile.TRANSFER,stage);stage=RefreshProfile.start();
            Structure structure=old==null?new Structure(false):old.structure;
            chunks.put(metadata.key(),new Chunk(metadata,structure));
            long vertices=terrain.address()+(long)metadata.row()*ROW_BYTES;
            structure.ensure(metadata.quads()*2,vertices,indices.address());
            if(!combined)vk.begin();structure.record(metadata.quads()*2,vertices,indices.address());barrier(VK_ACCESS_ACCELERATION_STRUCTURE_WRITE_BIT_KHR,VK_ACCESS_ACCELERATION_STRUCTURE_READ_BIT_KHR);vk.finish();builds++;
            RefreshProfile.end(RefreshProfile.BLAS,stage);
        }
        terrainRevision=source.revision();movingRevision=-1;
        System.out.println("RTX terrain synchronized: residentChunks="+chunks.size()+" rebuilt="+builds+" revision="+terrainRevision+" wallMs="+(System.nanoTime()-started)/1e6);
    }
    private Probe.Buffer terrainBuffer(long bytes) {
        if(bytes>vk.maxStorageBufferRange)throw new IllegalStateException("GPU storage-buffer range cannot address the retained terrain arena");
        return vk.buffer(bytes,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT|VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR|VK_BUFFER_USAGE_TRANSFER_DST_BIT|VK_BUFFER_USAGE_TRANSFER_SRC_BIT,false);
    }
    private void ensureIndices(int quads) {
        if(quads<=indexQuads)return;
        vk.release(indices);indexQuads=Integer.highestOneBit(Math.max(1,quads-1))<<1;
        indices=vk.buffer(indexQuads*24L,VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR,true);
        var data=indices.mapped().asIntBuffer();for(int q=0;q<indexQuads;q++)data.put(q*4).put(q*4+1).put(q*4+2).put(q*4+2).put(q*4+3).put(q*4);
    }
    void record() {
        if(!changed)return;
        barrier(VK_ACCESS_HOST_WRITE_BIT,VK_ACCESS_ACCELERATION_STRUCTURE_READ_BIT_KHR|VK_ACCESS_SHADER_READ_BIT);
        actors.record(actorCount,moving.address(),0);clouds.record(cloudCount,moving.address()+(cloudCount==0?0:actorCount*144L),0);
        barrier(VK_ACCESS_ACCELERATION_STRUCTURE_WRITE_BIT_KHR,VK_ACCESS_ACCELERATION_STRUCTURE_READ_BIT_KHR);
        top.record(chunks.size()+2,instances.address(),0);
        barrier(VK_ACCESS_ACCELERATION_STRUCTURE_WRITE_BIT_KHR,VK_ACCESS_ACCELERATION_STRUCTURE_READ_BIT_KHR);
        changed=false;
        if(++updateCount==1 || updateCount%600==0)System.out.println("RTX moving update="+updateCount+" actors="+actorCount+" clouds="+cloudCount+" terrainRevision="+terrainRevision);
    }
    private void barrier(int sourceAccess,int destinationAccess) {
        try(MemoryStack s=MemoryStack.stackPush()) {
            var barrier=VkMemoryBarrier.calloc(1,s).sType$Default().srcAccessMask(sourceAccess).dstAccessMask(destinationAccess);
            vkCmdPipelineBarrier(vk.command,VK_PIPELINE_STAGE_ALL_COMMANDS_BIT|VK_PIPELINE_STAGE_HOST_BIT,VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,0,barrier,null,null);
        }
    }
    final class Structure implements AutoCloseable {
        private final boolean isTop;
        private Probe.Buffer storage,scratch;
        long handle,address;private int capacity;
        Structure(boolean isTop){this.isTop=isTop;}
        private VkAccelerationStructureBuildGeometryInfoKHR.Buffer info(MemoryStack s,int count,long vertices,long index) {
            var geometry=VkAccelerationStructureGeometryKHR.calloc(1,s).sType$Default().geometryType(isTop?VK_GEOMETRY_TYPE_INSTANCES_KHR:VK_GEOMETRY_TYPE_TRIANGLES_KHR);
            if(isTop)geometry.geometry().instances().sType$Default().arrayOfPointers(false).data().deviceAddress(vertices);
            else {
                var triangles=geometry.geometry().triangles().sType$Default().vertexFormat(VK_FORMAT_R32G32B32_SFLOAT).vertexStride(48).maxVertex(Math.max(0,(index==0?count*3:count/2*4)-1)).indexType(index==0?VK_INDEX_TYPE_NONE_KHR:VK_INDEX_TYPE_UINT32);
                triangles.vertexData().deviceAddress(vertices);if(index!=0)triangles.indexData().deviceAddress(index);
            }
            // pGeometries does NOT set geometryCount in LWJGL (the native field
            // also sizes ppGeometries). A zero count silently builds empty BLAS.
            return VkAccelerationStructureBuildGeometryInfoKHR.calloc(1,s).sType$Default().type(isTop?VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR:VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR)
                .flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR).mode(VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR).geometryCount(1).pGeometries(geometry);
        }
        void ensure(int count,long vertices,long index) {
            if(handle!=0 && count<=capacity)return;
            close();capacity=Math.max(2,Integer.highestOneBit(Math.max(1,count-1))<<1);
            try(MemoryStack s=MemoryStack.stackPush()) {
                var info=info(s,capacity,vertices,index);var sizes=VkAccelerationStructureBuildSizesInfoKHR.calloc(s).sType$Default();
                vkGetAccelerationStructureBuildSizesKHR(vk.device,VK_ACCELERATION_STRUCTURE_BUILD_TYPE_DEVICE_KHR,info.get(0),s.ints(capacity),sizes);
                storage=vk.buffer(sizes.accelerationStructureSize(),VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_STORAGE_BIT_KHR,false);
                scratch=vk.buffer(sizes.buildScratchSize()+vk.scratchAlignment,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,false);
                var out=s.mallocLong(1);Probe.check(vkCreateAccelerationStructureKHR(vk.device,VkAccelerationStructureCreateInfoKHR.calloc(s).sType$Default().buffer(storage.handle()).size(storage.size()).type(info.type()),null,out));handle=out.get(0);
                address=vkGetAccelerationStructureDeviceAddressKHR(vk.device,VkAccelerationStructureDeviceAddressInfoKHR.calloc(s).sType$Default().accelerationStructure(handle));
            }
        }
        void record(int count,long vertices,long index) {
            try(MemoryStack s=MemoryStack.stackPush()) {
                var info=info(s,count,vertices,index).dstAccelerationStructure(handle);info.scratchData().deviceAddress(Probe.aligned(scratch.address(),vk.scratchAlignment));
                vkCmdBuildAccelerationStructuresKHR(vk.command,info,s.pointers(VkAccelerationStructureBuildRangeInfoKHR.calloc(s).primitiveCount(count).address()));
            }
        }
        public void close(){if(handle!=0)vkDestroyAccelerationStructureKHR(vk.device,handle,null);handle=address=0;vk.release(scratch);vk.release(storage);scratch=storage=null;capacity=0;}
    }
    public void close(){source.retainUpdates(false);top.close();actors.close();clouds.close();for(var chunk:chunks.values())chunk.structure.close();chunks.clear();vk.release(indices);vk.release(instances);vk.release(staging);vk.release(moving);vk.release(terrain);}
}
