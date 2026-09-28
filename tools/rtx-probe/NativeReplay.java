import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.*;

/** Real exported geometry/chords. Standalone comparison, not a Minecraft graphics backend. */
public final class NativeReplay {
    final Path scene,results;final ByteBuffer source;final int count,terrain;
    final int[] order,groups=new int[3],starts=new int[3],roots={-1,-1,-1};
    final float[] bounds;float[] nodes;int nodeCount;
    final ByteBuffer[] images=new ByteBuffer[3];
    Probe probe;Probe.Buffer vertices,treeBuffer,atlasBuffer;Probe.Acceleration top;
    NativeReplay(Path scene) throws Exception {
        this.scene=scene;results=scene.resolve("replay-results-"+System.currentTimeMillis()+".jsonl");source=map(scene.resolve("triangles.bin"));
        if(source.getInt(0)!=0x49525458||source.getInt(4)!=1)throw new IllegalArgumentException("Unknown geometry export");
        count=source.getInt(8);terrain=source.getInt(12);
        if(source.capacity()!=20L+count*144L||terrain<0||terrain>count)throw new IllegalArgumentException("Incomplete geometry export");
        order=new int[count];bounds=new float[count*6];nodes=new float[Math.max(64,count*4)];
        long started=System.nanoTime();
        for(int i=0;i<count;i++) {
            groups[group(i)]++;
            for(int axis=0;axis<3;axis++) {float lo=Float.POSITIVE_INFINITY,hi=Float.NEGATIVE_INFINITY;
                for(int corner=0;corner<3;corner++){float v=source.getFloat(20+i*144+corner*48+axis*4);if(!Float.isFinite(v))throw new IllegalArgumentException("Nonfinite native position");lo=Math.min(lo,v);hi=Math.max(hi,v);}
                bounds[i*6+axis]=lo;bounds[i*6+axis+3]=hi;
            }
        }
        starts[1]=groups[0];starts[2]=groups[0]+groups[1];int[] cursor=starts.clone();
        for(int i=0;i<count;i++)order[cursor[group(i)]++]=i;
        for(int i=0;i<3;i++)if(groups[i]>0)roots[i]=build(starts[i],starts[i]+groups[i]);
        for(int i=0;i<3;i++)images[i]=map(scene.resolve(new String[]{"atlas.bin","entities.bin","clouds.bin"}[i]));
        System.out.printf(Locale.ROOT,"NATIVE scene=%s triangles=%d groups=%s softwareNodes=%d cpuBuildMs=%.3f%n",scene,count,Arrays.toString(groups),nodeCount,(System.nanoTime()-started)/1e6);
    }
    static ByteBuffer map(Path path) throws Exception {try(var file=FileChannel.open(path)){return file.map(FileChannel.MapMode.READ_ONLY,0,file.size()).order(ByteOrder.LITTLE_ENDIAN);}}
    int group(int triangle){if(triangle<terrain)return 0;float tag=source.getFloat(20+triangle*144+12);return tag==5||tag==6?2:1;}
    int build(int first,int end) {
        int index=nodeCount++,p=index*8;if(p+8>nodes.length)nodes=Arrays.copyOf(nodes,Math.max(p+8,nodes.length*3/2));
        float[] lo={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY},hi={Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        for(int i=first;i<end;i++){int b=order[i]*6;for(int a=0;a<3;a++){lo[a]=Math.min(lo[a],bounds[b+a]);hi[a]=Math.max(hi[a],bounds[b+a+3]);}}
        for(int a=0;a<3;a++){nodes[p+a]=lo[a];nodes[p+4+a]=hi[a];}
        if(end-first<=8)nodes[p+7]=Float.intBitsToFloat((first<<4)|(end-first));
        else {int axis=0;for(int a=1;a<3;a++)if(hi[a]-lo[a]>hi[axis]-lo[axis])axis=a;float mid=(lo[axis]+hi[axis])*.5f;int a=first,b=end-1;
            while(a<=b){int q=order[a]*6;if((bounds[q+axis]+bounds[q+axis+3])*.5f<mid)a++;else{int t=order[a];order[a]=order[b];order[b--]=t;}}
            if(a==first||a==end)a=(first+end)/2;build(first,a);build(a,end);
        }
        nodes[p+3]=Float.intBitsToFloat(nodeCount);return index;
    }
    void upload() throws Exception {
        upload(null);
    }
    void upload(Probe context) throws Exception {
        probe=context==null?new Probe("tools/rtx-probe/native-query.comp",5,20,48,false):context;
        try(MemoryStack s=MemoryStack.stackPush()) {var properties=VkPhysicalDeviceProperties.calloc(s);vkGetPhysicalDeviceProperties(probe.physical,properties);
            long limit=Integer.toUnsignedLong(properties.limits().maxStorageBufferRange());System.out.println("LIMIT maxStorageBufferRange="+limit);
            if(count*144L>limit)throw new IllegalStateException("Native vertex buffer exceeds descriptor range; split buffers or device-address access required");}
        vertices=probe.buffer(count*144L,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT|VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR|VK_BUFFER_USAGE_TRANSFER_DST_BIT,false);
        final int chunk=131072;var staging=probe.buffer(chunk*144L,VK_BUFFER_USAGE_TRANSFER_SRC_BIT,true);double uploadGpu=0;
        for(int first=0;first<count;first+=chunk) {
            int length=Math.min(chunk,count-first);
            for(int i=0;i<length;i++) {
                int original=order[first+i];MemoryUtil.memCopy(MemoryUtil.memAddress(source)+20L+original*144L,MemoryUtil.memAddress(staging.mapped())+i*144L,144);
                float tag=source.getFloat(20+original*144+12);if(Math.abs(tag)>=128)throw new IllegalStateException("Unexpected native material tag");
                staging.mapped().putFloat(i*144+12,(1<<group(original))*256+128+tag);
            }
            try(MemoryStack s=MemoryStack.stackPush()) {probe.begin();vkCmdCopyBuffer(probe.command,staging.handle(),vertices.handle(),VkBufferCopy.calloc(1,s).srcOffset(0).dstOffset(first*144L).size(length*144L));
                var barrier=VkMemoryBarrier.calloc(1,s).sType$Default().srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT|VK_ACCESS_ACCELERATION_STRUCTURE_READ_BIT_KHR);
                vkCmdPipelineBarrier(probe.command,VK_PIPELINE_STAGE_TRANSFER_BIT,VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT|VK_PIPELINE_STAGE_ACCELERATION_STRUCTURE_BUILD_BIT_KHR,0,barrier,null,null);uploadGpu+=probe.finish();}
        }
        System.out.printf(Locale.ROOT,"UPLOAD geometryBytes=%d stagingBytes=%d copyGpuMs=%.6f%n",count*144L,staging.size(),uploadGpu);
        if(context==null) {
        treeBuffer=probe.buffer(nodeCount*32L,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,true);treeBuffer.mapped().asFloatBuffer().put(nodes,0,nodeCount*8);
        int words=12;for(var image:images){if(image.capacity()!=8L+image.getInt(0)*image.getInt(4)*4L)throw new IllegalArgumentException("Bad atlas");words+=(image.capacity()-8)/4;}
        atlasBuffer=probe.buffer(words*4L,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,true);int offset=12;
        for(int i=0;i<3;i++){var image=images[i];var out=atlasBuffer.mapped();out.putInt(i*16,image.getInt(0));out.putInt(i*16+4,image.getInt(4));out.putInt(i*16+8,offset);
            MemoryUtil.memCopy(MemoryUtil.memAddress(image)+8,MemoryUtil.memAddress(out)+offset*4L,image.capacity()-8);offset+=(image.capacity()-8)/4;}
        }
        Probe.Acceleration[] bottoms=new Probe.Acceleration[3];double gpuBuild=0;long storage=0;
        for(int i=0;i<3;i++)if(groups[i]>0){var slice=new Probe.Buffer(vertices.handle(),vertices.memory(),vertices.address()+starts[i]*144L,null,groups[i]*144L);bottoms[i]=probe.build(slice,groups[i],null);gpuBuild+=bottoms[i].gpuMs();storage+=bottoms[i].bytes();}
        top=probe.instances(bottoms,starts);
        System.out.printf(Locale.ROOT,"AS bottomBuildGpuMs=%.6f topBuildGpuMs=%.6f storageBytes=%d%n",gpuBuild,top.gpuMs(),storage+top.bytes());
    }
    double dispatch(long pipeline,int count,int alpha,int batch) {try(MemoryStack s=MemoryStack.stackPush()){
        probe.begin();vkCmdBindPipeline(probe.command,VK_PIPELINE_BIND_POINT_COMPUTE,pipeline);vkCmdBindDescriptorSets(probe.command,VK_PIPELINE_BIND_POINT_COMPUTE,probe.layout,0,s.longs(probe.set),null);
        vkCmdPushConstants(probe.command,probe.layout,VK_SHADER_STAGE_COMPUTE_BIT,0,s.ints(count,alpha,roots[0],roots[1],roots[2]));
        var b=VkMemoryBarrier.calloc(1,s).sType$Default().srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT);
        for(int i=0;i<batch;i++){vkCmdDispatch(probe.command,(count+63)/64,1,1);if(i+1<batch)vkCmdPipelineBarrier(probe.command,VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,0,b,null,null);}
        b.dstAccessMask(VK_ACCESS_HOST_READ_BIT);vkCmdPipelineBarrier(probe.command,VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,VK_PIPELINE_STAGE_HOST_BIT,0,b,null,null);return probe.finish()/batch;
    }}
    boolean cpuBounds(int node,ByteBuffer rays,int offset,double best){double enter=0,leave=best;for(int a=0;a<3;a++){
        double o=rays.getFloat(offset+a*4),d=rays.getFloat(offset+16+a*4),lo=nodes[node*8+a],hi=nodes[node*8+4+a];
        if(Math.abs(d)<1e-12){if(o<lo||o>hi)return false;}else{double t0=(lo-o)/d,t1=(hi-o)/d;enter=Math.max(enter,Math.min(t0,t1));leave=Math.min(leave,Math.max(t0,t1));}}
        return leave>=enter;
    }
    double cpuTriangle(int tri,ByteBuffer rays,int offset,int alpha,double best){
        int base=20+order[tri]*144;double[] a=new double[3],e1=new double[3],e2=new double[3],d=new double[3],s=new double[3];
        for(int i=0;i<3;i++){a[i]=source.getFloat(base+i*4);e1[i]=source.getFloat(base+48+i*4)-a[i];e2[i]=source.getFloat(base+96+i*4)-a[i];d[i]=rays.getFloat(offset+16+i*4);s[i]=rays.getFloat(offset+i*4)-a[i];}
        double[] p={d[1]*e2[2]-d[2]*e2[1],d[2]*e2[0]-d[0]*e2[2],d[0]*e2[1]-d[1]*e2[0]};
        double det=e1[0]*p[0]+e1[1]*p[1]+e1[2]*p[2];float tag=source.getFloat(base+12);if(Math.abs(tag)>=64)tag-=Math.signum(tag)*64;if(tag==-4)tag=0;if(Math.abs(tag)>=32)return -1;
        double mode=Math.abs(tag);boolean cloud=mode==5||mode==6,twoSided=mode==2||mode==6||mode>=7&&mode%2==0;
        if((rays.getInt(offset+44)&(1<<group(order[tri])))==0||cloud&&(rays.getInt(offset+40)&2)==0||(twoSided?Math.abs(det)<1e-10:det<1e-10))return -1;
        double u=(s[0]*p[0]+s[1]*p[1]+s[2]*p[2])/det;if(u<0||u>1)return -1;
        double[] q={s[1]*e1[2]-s[2]*e1[1],s[2]*e1[0]-s[0]*e1[2],s[0]*e1[1]-s[1]*e1[0]};
        double v=(d[0]*q[0]+d[1]*q[1]+d[2]*q[2])/det;if(v<0||u+v>1)return -1;
        double t=(e2[0]*q[0]+e2[1]*q[1]+e2[2]*q[2])/det;if(t<0||t>1||t>=best)return -1;
        if(alpha!=0){double[] weights={1-u-v,u,v};double ux=0,uy=0,vertexAlpha=0;for(int i=0;i<3;i++){ux+=weights[i]*source.getFloat(base+i*48+16);uy+=weights[i]*source.getFloat(base+i*48+20);vertexAlpha+=weights[i]*source.getFloat(base+i*48+44);}
            var img=images[cloud?2:tag>0?1:0];int w=img.getInt(0),h=img.getInt(4);
            if(mode==13||mode==14||mode==17||mode==18){double zw=source.getFloat(base+24),ww=source.getFloat(base+28);ux=(zw%4096+(ux-Math.floor(ux))*Math.floor(zw/4096))/w;uy=(ww%4096+(uy-Math.floor(uy))*Math.floor(ww/4096))/h;}
            int x=Math.max(0,Math.min(w-1,(int)Math.floor(ux*w))),y=Math.max(0,Math.min(h-1,(int)Math.floor(uy*h)));int rgba=img.getInt(8+(y*w+x)*4);double opacity=(mode==15||mode==16?rgba&255:rgba>>>24)/255.0;if(cloud)opacity*=vertexAlpha;
            if(opacity<(mode==9||mode==10?.001:.1))return -1;
        }
        return t;
    }
    double cpu(ByteBuffer rays,int ray,int alpha){int offset=16+ray*48;double best=1.000001;boolean found=false;for(int root:roots){if(root<0)continue;int node=root,end=Float.floatToRawIntBits(nodes[root*8+3]);while(node<end){int escape=Float.floatToRawIntBits(nodes[node*8+3]);if(!cpuBounds(node,rays,offset,best)){node=escape;continue;}int header=Float.floatToRawIntBits(nodes[node*8+7]),size=header&15;
        if(size==0){node++;continue;}for(int i=0;i<size;i++){double t=cpuTriangle((header>>>4)+i,rays,offset,alpha,best);if(t>=0){best=t;found=true;}}node=escape;}}
        return found?best:-1;
    }
    static boolean same(double a,double b){return (a<0)==(b<0)&&(a<0||Math.abs(a-b)<=1e-4+2e-5*Math.abs(a));}
    static boolean edge(float[] hit,int index){float t=hit[index],u=hit[index+2],v=hit[index+3];return t>=0&&(t<1e-5||t>1-1e-5||u<1e-5||v<1e-5||u+v>1-1e-5);}
    void run(Path view) throws Exception {
        for(String pass:new String[]{"probe","mask"}) {
            ByteBuffer rays=map(view.resolve(pass+".bin"));int n=rays.getInt(8);
            if(rays.getInt(0)!=0x49525259||rays.getInt(4)!=1||rays.getInt(12)!=48||rays.capacity()!=16L+n*48L)throw new IllegalArgumentException("Bad ray export");
            if(n==0){System.out.println("EMPTY "+view+" "+pass);continue;}
            var input=probe.buffer(n*48L,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,true);MemoryUtil.memCopy(MemoryUtil.memAddress(rays)+16,MemoryUtil.memAddress(input.mapped()),n*48L);
            var output=probe.buffer(n*16L,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,true);probe.descriptors(top,vertices,treeBuffer,input,output,atlasBuffer);
            int[] masks=new int[8];for(int i=0;i<n;i++)masks[rays.getInt(16+i*48+44)]++;
            for(int alpha=0;alpha<=1;alpha++){
                float[] sw=new float[n*4],hw=new float[n*4];dispatch(probe.software,n,alpha,1);output.mapped().asFloatBuffer().get(sw);dispatch(probe.hardware,n,alpha,1);output.mapped().asFloatBuffer().get(hw);
                int hits=0,edges=0,failures=0,cpuFailures=0,productionDifferences=0,ties=0;double maxError=0;
                for(int i=0;i<n;i++){int at=i*4;if(hw[at]>=0)hits++;
                    if(!Float.isFinite(hw[at])||!Float.isFinite(sw[at]))throw new IllegalStateException("Nonfinite query answer");
                    if(!same(sw[at],hw[at])){double cpu=cpu(rays,i,alpha);boolean resolved=(same(cpu,hw[at])||same(cpu,sw[at]))&&(edge(sw,at)||edge(hw,at));if(resolved)edges++;else failures++;
                        if(edges+failures<=20)System.out.printf(Locale.ROOT,"DIFFERENCE pass=%s alpha=%d ray=%d sw=%g hw=%g cpu=%g boundary=%s%n",pass,alpha,i,sw[at],hw[at],cpu,resolved);
                    }else if(hw[at]>=0){maxError=Math.max(maxError,Math.abs(sw[at]-hw[at]));if(sw[at+1]!=hw[at+1])ties++;}
                    if(alpha==1&&!same(rays.getFloat(16+i*48+12),hw[at])) {
                        productionDifferences++;double expected=rays.getFloat(16+i*48+12),c=cpu(rays,i,alpha);
                        boolean boundary=edge(hw,at)||expected>=0&&(expected<1e-5||expected>1-1e-5);
                        if(!boundary||!same(c,hw[at])&&!same(c,expected))failures++;
                        if(productionDifferences<=12)System.out.printf(Locale.ROOT,"PRODUCTION_DIFFERENCE pass=%s ray=%d recorded=%g hw=%g cpu=%g boundary=%s%n",pass,i,expected,hw[at],c,boundary);
                    }
                }
                for(int i=0;i<64;i++){int ray=(int)((i*7919L+37)%n);double c=cpu(rays,ray,alpha);if(!same(c,hw[ray*4])&&!(same(c,sw[ray*4])&&(edge(sw,ray*4)||edge(hw,ray*4))))cpuFailures++;}
                System.out.printf(Locale.ROOT,"CHECK view=%s pass=%s alpha=%d queries=%d masks=%s hits=%d pairedFailures=%d boundaryDifferences=%d cpuFailures=%d productionDifferences=%d coplanarIdTies=%d maxTError=%g%n",view.getFileName(),pass,alpha,n,Arrays.toString(masks),hits,failures,edges,cpuFailures,productionDifferences,ties,maxError);
                if(failures+cpuFailures>0)throw new IllegalStateException("Unexplained native replay differences; timing suppressed");
                for(int i=0;i<8;i++){dispatch(probe.software,n,alpha,4);dispatch(probe.hardware,n,alpha,4);}
                double[] software=new double[24],hardware=new double[24];
                for(int i=0;i<24;i++){if(i%2==0){software[i]=dispatch(probe.software,n,alpha,8);hardware[i]=dispatch(probe.hardware,n,alpha,8);}else{hardware[i]=dispatch(probe.hardware,n,alpha,8);software[i]=dispatch(probe.software,n,alpha,8);}}
                String line=String.format(Locale.ROOT,"{\"scene\":\"%s\",\"view\":\"%s\",\"pass\":\"%s\",\"alpha\":%d,\"triangles\":%d,\"queries\":%d,\"hits\":%d,\"boundaryDifferences\":%d,\"productionDifferences\":%d,\"softwareMedianMs\":%.6f,\"hardwareMedianMs\":%.6f,\"softwareSamplesMs\":%s,\"hardwareSamplesMs\":%s}%n",scene.getFileName(),view.getFileName(),pass,alpha,count,n,hits,edges,productionDifferences,Probe.percentile(software,.5),Probe.percentile(hardware,.5),Arrays.toString(software),Arrays.toString(hardware));
                Files.writeString(results,line,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                System.out.printf(Locale.ROOT,"TIMING pass=%s alpha=%d softwareMs=%.6f hardwareMs=%.6f speedup=%.3f%n",pass,alpha,Probe.percentile(software,.5),Probe.percentile(hardware,.5),Probe.percentile(software,.5)/Probe.percentile(hardware,.5));
            }
        }
    }
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);NativeReplay replay=new NativeReplay(Path.of(args[0]));
        try {replay.upload();try(var files=Files.list(replay.scene)){for(Path view:files.filter(p->Files.isDirectory(p)&&p.getFileName().toString().startsWith("view-")).sorted().toList())replay.run(view);}}
        finally {if(replay.probe!=null)replay.probe.close();}
        System.out.println("RESULTS "+replay.results.toAbsolutePath());
    }
}
