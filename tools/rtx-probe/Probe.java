import org.lwjgl.*;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.*;
import static org.lwjgl.vulkan.KHRRayQuery.*;
import static org.lwjgl.util.shaderc.Shaderc.*;

/** Standalone intersection microbenchmark. No Minecraft/world access or production renderer changes. */
public final class Probe implements AutoCloseable {
    static final int QUERIES=262144, WARMUP=12, ROUNDS=30, BATCH=16;
    VkInstance instance; VkPhysicalDevice physical; VkDevice device; VkQueue queue;
    VkCommandBuffer command; VkPhysicalDeviceMemoryProperties memory;
    long pool,queryPool,layout,descriptorLayout,descriptorPool,set,hardware,software;
    float timestampPeriod; int queueFamily,scratchAlignment; final List<Runnable> cleanup=new ArrayList<>();
    record Buffer(long handle,long memory,long address,ByteBuffer mapped,long size) {}
    record Acceleration(long handle,long address,double gpuMs,long bytes) {}
    static void check(int result){if(result!=VK_SUCCESS)throw new IllegalStateException("Vulkan error "+result);}
    static long aligned(long n,long a){return (n+a-1)&-a;}

    Probe() throws Exception {
        try(MemoryStack s=MemoryStack.stackPush()) {
            var app=VkApplicationInfo.calloc(s).sType$Default().pApplicationName(s.UTF8("Interstellar RTX probe")).apiVersion(VK_API_VERSION_1_2);
            var create=VkInstanceCreateInfo.calloc(s).sType$Default().pApplicationInfo(app);
            PointerBuffer p=s.mallocPointer(1);check(vkCreateInstance(create,null,p));instance=new VkInstance(p.get(0),create);
            var count=s.ints(0);check(vkEnumeratePhysicalDevices(instance,count,null));var devices=s.mallocPointer(count.get(0));check(vkEnumeratePhysicalDevices(instance,count,devices));
            for(int i=0;i<count.get(0);i++){
                var candidate=new VkPhysicalDevice(devices.get(i),instance);var props=VkPhysicalDeviceProperties.calloc(s);vkGetPhysicalDeviceProperties(candidate,props);
                if(props.deviceType()==VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU){physical=candidate;break;}
            }
            if(physical==null)throw new IllegalStateException("No discrete Vulkan GPU");
            var asProps=VkPhysicalDeviceAccelerationStructurePropertiesKHR.calloc(s).sType$Default();
            var props=VkPhysicalDeviceProperties2.calloc(s).sType$Default().pNext(asProps.address());vkGetPhysicalDeviceProperties2(physical,props);
            timestampPeriod=props.properties().limits().timestampPeriod();scratchAlignment=asProps.minAccelerationStructureScratchOffsetAlignment();
            System.out.printf(Locale.ROOT,"DEVICE name=%s timestampNs=%.3f scratchAlignment=%d%n",props.properties().deviceNameString(),timestampPeriod,scratchAlignment);
            var ray=VkPhysicalDeviceRayQueryFeaturesKHR.calloc(s).sType$Default();
            var accel=VkPhysicalDeviceAccelerationStructureFeaturesKHR.calloc(s).sType$Default().pNext(ray.address());
            var v12=VkPhysicalDeviceVulkan12Features.calloc(s).sType$Default().pNext(accel.address());
            var features=VkPhysicalDeviceFeatures2.calloc(s).sType$Default().pNext(v12.address());vkGetPhysicalDeviceFeatures2(physical,features);
            if(!ray.rayQuery()||!accel.accelerationStructure()||!v12.bufferDeviceAddress())throw new IllegalStateException("Required RT features unavailable");
            ray.clear();ray.sType$Default().rayQuery(true);accel.clear();accel.sType$Default().pNext(ray.address()).accelerationStructure(true);v12.clear();v12.sType$Default().pNext(accel.address()).bufferDeviceAddress(true);
            vkGetPhysicalDeviceQueueFamilyProperties(physical,count,null);var queues=VkQueueFamilyProperties.calloc(count.get(0),s);vkGetPhysicalDeviceQueueFamilyProperties(physical,count,queues);
            queueFamily=-1;for(int i=0;i<count.get(0);i++)if((queues.get(i).queueFlags()&VK_QUEUE_COMPUTE_BIT)!=0&&queues.get(i).timestampValidBits()==64){queueFamily=i;break;}
            if(queueFamily<0)throw new IllegalStateException("No compute queue with 64-bit timestamps");
            var queueInfo=VkDeviceQueueCreateInfo.calloc(1,s).sType$Default().queueFamilyIndex(queueFamily).pQueuePriorities(s.floats(1));
            var deviceInfo=VkDeviceCreateInfo.calloc(s).sType$Default().pNext(v12.address()).pQueueCreateInfos(queueInfo)
                .ppEnabledExtensionNames(s.pointers(s.UTF8("VK_KHR_acceleration_structure"),s.UTF8("VK_KHR_deferred_host_operations"),s.UTF8("VK_KHR_ray_query")));
            check(vkCreateDevice(physical,deviceInfo,null,p));device=new VkDevice(p.get(0),physical,deviceInfo);vkGetDeviceQueue(device,queueFamily,0,p);queue=new VkQueue(p.get(0),device);
            memory=VkPhysicalDeviceMemoryProperties.calloc();vkGetPhysicalDeviceMemoryProperties(physical,memory);
            var out=s.mallocLong(1);
            check(vkCreateCommandPool(device,VkCommandPoolCreateInfo.calloc(s).sType$Default().queueFamilyIndex(queueFamily).flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT),null,out));pool=out.get(0);
            check(vkAllocateCommandBuffers(device,VkCommandBufferAllocateInfo.calloc(s).sType$Default().commandPool(pool).level(VK_COMMAND_BUFFER_LEVEL_PRIMARY).commandBufferCount(1),p));command=new VkCommandBuffer(p.get(0),device);
            check(vkCreateQueryPool(device,VkQueryPoolCreateInfo.calloc(s).sType$Default().queryType(VK_QUERY_TYPE_TIMESTAMP).queryCount(2),null,out));queryPool=out.get(0);
            var bindings=VkDescriptorSetLayoutBinding.calloc(5,s);
            for(int i=0;i<5;i++)bindings.get(i).binding(i).descriptorType(i==0?VK_DESCRIPTOR_TYPE_ACCELERATION_STRUCTURE_KHR:VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(1).stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
            check(vkCreateDescriptorSetLayout(device,VkDescriptorSetLayoutCreateInfo.calloc(s).sType$Default().pBindings(bindings),null,out));descriptorLayout=out.get(0);
            var push=VkPushConstantRange.calloc(1,s).stageFlags(VK_SHADER_STAGE_COMPUTE_BIT).offset(0).size(4);
            check(vkCreatePipelineLayout(device,VkPipelineLayoutCreateInfo.calloc(s).sType$Default().pSetLayouts(s.longs(descriptorLayout)).pPushConstantRanges(push),null,out));layout=out.get(0);
            var sizes=VkDescriptorPoolSize.calloc(2,s);sizes.get(0).type(VK_DESCRIPTOR_TYPE_ACCELERATION_STRUCTURE_KHR).descriptorCount(1);sizes.get(1).type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(4);
            check(vkCreateDescriptorPool(device,VkDescriptorPoolCreateInfo.calloc(s).sType$Default().maxSets(1).pPoolSizes(sizes),null,out));descriptorPool=out.get(0);
            check(vkAllocateDescriptorSets(device,VkDescriptorSetAllocateInfo.calloc(s).sType$Default().descriptorPool(descriptorPool).pSetLayouts(s.longs(descriptorLayout)),out));set=out.get(0);
        }
        software=pipeline(false);hardware=pipeline(true);
    }

    Buffer buffer(long bytes,int usage,boolean host) {
        try(MemoryStack s=MemoryStack.stackPush()) {
            var out=s.mallocLong(1);check(vkCreateBuffer(device,VkBufferCreateInfo.calloc(s).sType$Default().size(bytes).usage(usage|VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT).sharingMode(VK_SHARING_MODE_EXCLUSIVE),null,out));long handle=out.get(0);
            var requirements=VkMemoryRequirements.calloc(s);vkGetBufferMemoryRequirements(device,handle,requirements);
            int required=host?VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT:VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
            int type=-1;
            for(int pass=0;pass<2&&type<0;pass++)for(int i=0;i<memory.memoryTypeCount();i++){
                int flags=memory.memoryTypes(i).propertyFlags();
                if((requirements.memoryTypeBits()&(1<<i))!=0&&(flags&required)==required&&(pass==1||(flags&VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)!=0)){type=i;break;}
            }
            if(type<0)throw new IllegalStateException("No suitable memory type");
            var allocationFlags=VkMemoryAllocateFlagsInfo.calloc(s).sType$Default().flags(VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT);
            check(vkAllocateMemory(device,VkMemoryAllocateInfo.calloc(s).sType$Default().pNext(allocationFlags.address()).allocationSize(requirements.size()).memoryTypeIndex(type),null,out));long allocation=out.get(0);check(vkBindBufferMemory(device,handle,allocation,0));
            ByteBuffer mapped=null;if(host){var pointer=s.mallocPointer(1);check(vkMapMemory(device,allocation,0,bytes,0,pointer));mapped=memByteBuffer(pointer.get(0),Math.toIntExact(bytes));}
            long address=vkGetBufferDeviceAddress(device,VkBufferDeviceAddressInfo.calloc(s).sType$Default().buffer(handle));
            cleanup.add(()->{if(host)vkUnmapMemory(device,allocation);vkDestroyBuffer(device,handle,null);vkFreeMemory(device,allocation,null);});
            if(host)System.out.printf("BUFFER bytes=%d memoryType=%d flags=0x%x%n",bytes,type,memory.memoryTypes(type).propertyFlags());
            return new Buffer(handle,allocation,address,mapped,bytes);
        }
    }

    void begin(){check(vkResetCommandBuffer(command,0));try(MemoryStack s=MemoryStack.stackPush()){check(vkBeginCommandBuffer(command,VkCommandBufferBeginInfo.calloc(s).sType$Default().flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)));}vkCmdResetQueryPool(command,queryPool,0,2);vkCmdWriteTimestamp(command,VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,queryPool,0);}
    double finish() {
        vkCmdWriteTimestamp(command,VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,queryPool,1);check(vkEndCommandBuffer(command));
        try(MemoryStack s=MemoryStack.stackPush()){
            check(vkQueueSubmit(queue,VkSubmitInfo.calloc(s).sType$Default().pCommandBuffers(s.pointers(command.address())),VK_NULL_HANDLE));check(vkQueueWaitIdle(queue));
            var timestamps=s.mallocLong(2);check(vkGetQueryPoolResults(device,queryPool,0,2,timestamps,8,VK_QUERY_RESULT_64_BIT|VK_QUERY_RESULT_WAIT_BIT));return (timestamps.get(1)-timestamps.get(0))*timestampPeriod/1e6;
        }
    }
    Acceleration build(Buffer vertices,int triangles,Acceleration bottom) {
        try(MemoryStack s=MemoryStack.stackPush()) {
            boolean top=bottom!=null;Buffer input=vertices;
            var geometry=VkAccelerationStructureGeometryKHR.calloc(1,s).sType$Default().flags(VK_GEOMETRY_OPAQUE_BIT_KHR).geometryType(top?VK_GEOMETRY_TYPE_INSTANCES_KHR:VK_GEOMETRY_TYPE_TRIANGLES_KHR);
            if(top){input=buffer(64,VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR,true);var inst=VkAccelerationStructureInstanceKHR.create(memAddress(input.mapped));for(int i=0;i<12;i++)inst.transform().matrix(i,i==0||i==5||i==10?1:0);inst.instanceCustomIndex(0).mask(255).instanceShaderBindingTableRecordOffset(0).flags(VK_GEOMETRY_INSTANCE_TRIANGLE_FACING_CULL_DISABLE_BIT_KHR).accelerationStructureReference(bottom.address);geometry.geometry().instances().sType$Default().arrayOfPointers(false).data().deviceAddress(input.address);}
            else geometry.geometry().triangles().sType$Default().vertexFormat(VK_FORMAT_R32G32B32_SFLOAT).vertexStride(16).maxVertex(triangles*3-1).indexType(VK_INDEX_TYPE_NONE_KHR).vertexData().deviceAddress(input.address);
            int count=top?1:triangles;
            var info=VkAccelerationStructureBuildGeometryInfoKHR.calloc(1,s).sType$Default().type(top?VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR:VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR).flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR).mode(VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR).geometryCount(1).pGeometries(geometry);
            var sizes=VkAccelerationStructureBuildSizesInfoKHR.calloc(s).sType$Default();vkGetAccelerationStructureBuildSizesKHR(device,VK_ACCELERATION_STRUCTURE_BUILD_TYPE_DEVICE_KHR,info.get(0),s.ints(count),sizes);
            Buffer storage=buffer(sizes.accelerationStructureSize(),VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_STORAGE_BIT_KHR,false),scratch=buffer(sizes.buildScratchSize()+scratchAlignment,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,false);
            var out=s.mallocLong(1);check(vkCreateAccelerationStructureKHR(device,VkAccelerationStructureCreateInfoKHR.calloc(s).sType$Default().buffer(storage.handle).size(storage.size).type(info.type()),null,out));long handle=out.get(0);cleanup.add(()->vkDestroyAccelerationStructureKHR(device,handle,null));
            info.dstAccelerationStructure(handle).scratchData().deviceAddress(aligned(scratch.address,scratchAlignment));
            var range=VkAccelerationStructureBuildRangeInfoKHR.calloc(s).primitiveCount(count);
            begin();vkCmdBuildAccelerationStructuresKHR(command,info,s.pointers(range.address()));
            var barrier=VkMemoryBarrier.calloc(1,s).sType$Default().srcAccessMask(VK_ACCESS_ACCELERATION_STRUCTURE_WRITE_BIT_KHR).dstAccessMask(VK_ACCESS_ACCELERATION_STRUCTURE_READ_BIT_KHR);
            vkCmdPipelineBarrier(command,VK_PIPELINE_STAGE_ACCELERATION_STRUCTURE_BUILD_BIT_KHR,VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT|VK_PIPELINE_STAGE_ACCELERATION_STRUCTURE_BUILD_BIT_KHR,0,barrier,null,null);
            double ms=finish();long address=vkGetAccelerationStructureDeviceAddressKHR(device,VkAccelerationStructureDeviceAddressInfoKHR.calloc(s).sType$Default().accelerationStructure(handle));return new Acceleration(handle,address,ms,sizes.accelerationStructureSize());
        }
    }
    long pipeline(boolean rt) throws Exception {
        String source=Files.readString(Path.of("tools/rtx-probe/query.comp"));if(rt)source=source.replace("#version 460","#version 460\n#define HARDWARE");
        long compiler=shaderc_compiler_initialize(),options=shaderc_compile_options_initialize();
        shaderc_compile_options_set_target_env(options,shaderc_target_env_vulkan,shaderc_env_version_vulkan_1_2);shaderc_compile_options_set_optimization_level(options,shaderc_optimization_level_performance);
        long result=shaderc_compile_into_spv(compiler,source,shaderc_compute_shader,"query.comp","main",options);
        try(MemoryStack s=MemoryStack.stackPush()){
            if(shaderc_result_get_compilation_status(result)!=shaderc_compilation_status_success)throw new IllegalStateException(shaderc_result_get_error_message(result));
            var out=s.mallocLong(1);check(vkCreateShaderModule(device,VkShaderModuleCreateInfo.calloc(s).sType$Default().pCode(shaderc_result_get_bytes(result)),null,out));long module=out.get(0);
            var stage=VkPipelineShaderStageCreateInfo.calloc(s).sType$Default().stage(VK_SHADER_STAGE_COMPUTE_BIT).module(module).pName(s.UTF8("main"));
            var info=VkComputePipelineCreateInfo.calloc(1,s).sType$Default().stage(stage).layout(layout);check(vkCreateComputePipelines(device,VK_NULL_HANDLE,info,null,out));long pipeline=out.get(0);vkDestroyShaderModule(device,module,null);return pipeline;
        }finally{shaderc_result_release(result);shaderc_compile_options_release(options);shaderc_compiler_release(compiler);}
    }
    void descriptors(Acceleration top,Buffer... buffers) {
        try(MemoryStack s=MemoryStack.stackPush()){
            var writes=VkWriteDescriptorSet.calloc(5,s);var as=VkWriteDescriptorSetAccelerationStructureKHR.calloc(s).sType$Default().pAccelerationStructures(s.longs(top.handle));
            writes.get(0).sType$Default().dstSet(set).dstBinding(0).descriptorCount(1).descriptorType(VK_DESCRIPTOR_TYPE_ACCELERATION_STRUCTURE_KHR).pNext(as.address());
            for(int i=0;i<4;i++){var info=VkDescriptorBufferInfo.calloc(1,s).buffer(buffers[i].handle).offset(0).range(buffers[i].size);writes.get(i+1).sType$Default().dstSet(set).dstBinding(i+1).descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(1).pBufferInfo(info);}
            vkUpdateDescriptorSets(device,writes,null);
        }
    }
    double dispatch(long pipeline,int count){
        try(MemoryStack s=MemoryStack.stackPush()){
            begin();vkCmdBindPipeline(command,VK_PIPELINE_BIND_POINT_COMPUTE,pipeline);vkCmdBindDescriptorSets(command,VK_PIPELINE_BIND_POINT_COMPUTE,layout,0,s.longs(set),null);vkCmdPushConstants(command,layout,VK_SHADER_STAGE_COMPUTE_BIT,0,s.ints(count));
            var barrier=VkMemoryBarrier.calloc(1,s).sType$Default().srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT);
            for(int i=0;i<BATCH;i++){vkCmdDispatch(command,(count+63)/64,1,1);if(i+1<BATCH)vkCmdPipelineBarrier(command,VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,0,barrier,null,null);}
            barrier.dstAccessMask(VK_ACCESS_HOST_READ_BIT);vkCmdPipelineBarrier(command,VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,VK_PIPELINE_STAGE_HOST_BIT,0,barrier,null,null);return finish()/BATCH;
        }
    }

    record Triangle(float[] p) {float centre(int axis){return (p[axis]+p[axis+3]+p[axis+6])/3;}}
    static final class Node {float[] lo={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY},hi={Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};int escape,first,count;}
    static int tree(Triangle[] triangles,int first,int end,List<Node> nodes){
        int index=nodes.size();Node node=new Node();nodes.add(node);node.first=first;
        for(int i=first;i<end;i++)for(int corner=0;corner<3;corner++)for(int axis=0;axis<3;axis++){float v=triangles[i].p[corner*3+axis];node.lo[axis]=Math.min(node.lo[axis],v);node.hi[axis]=Math.max(node.hi[axis],v);}
        if(end-first<=8)node.count=end-first;
        else {int axis=0;for(int j=1;j<3;j++)if(node.hi[j]-node.lo[j]>node.hi[axis]-node.lo[axis])axis=j;float middle=(node.lo[axis]+node.hi[axis])/2;int a=first,b=end-1;while(a<=b){if(triangles[a].centre(axis)<middle)a++;else{Triangle tmp=triangles[a];triangles[a]=triangles[b];triangles[b--]=tmp;}}if(a==first||a==end)a=(first+end)/2;tree(triangles,first,a,nodes);tree(triangles,a,end,nodes);}
        node.escape=nodes.size();return index;
    }
    static void quad(List<Triangle> list,float[] a,float[] b,float[] c,float[] d){list.add(new Triangle(new float[]{a[0],a[1],a[2],b[0],b[1],b[2],c[0],c[1],c[2]}));list.add(new Triangle(new float[]{c[0],c[1],c[2],d[0],d[1],d[2],a[0],a[1],a[2]}));}
    static float[] xyz(float x,float y,float z){return new float[]{x,y,z};}
    static float height(int x,int z){return (float)Math.floor(3*Math.sin(x*.17)+4*Math.cos(z*.11)+2*Math.sin((x+z)*.27));}
    static Triangle[] scene(int width){
        List<Triangle> list=new ArrayList<>();int start=-width/2;
        for(int x=start;x<start+width;x++)for(int z=start;z<start+width;z++){
            float h=height(x,z);quad(list,xyz(x,h,z),xyz(x+1,h,z),xyz(x+1,h,z+1),xyz(x,h,z+1));
            float e=height(x+1,z);if(e!=h)quad(list,xyz(x+1,h,z),xyz(x+1,e,z),xyz(x+1,e,z+1),xyz(x+1,h,z+1));
            float n=height(x,z+1);if(n!=h)quad(list,xyz(x,h,z+1),xyz(x+1,h,z+1),xyz(x+1,n,z+1),xyz(x,n,z+1));
        }
        Random r=new Random(8432);for(int i=0;i<128;i++)box(list,r.nextFloat()*width-width/2f,r.nextFloat()*8+2,r.nextFloat()*width-width/2f,.8f,1.8f,.8f);
        for(int i=0;i<32;i++)box(list,r.nextFloat()*width-width/2f,25+r.nextFloat()*4,r.nextFloat()*width-width/2f,8,2,5);
        return list.toArray(Triangle[]::new);
    }
    static void box(List<Triangle> l,float x,float y,float z,float w,float h,float d){
        float[][] p={xyz(x,y,z),xyz(x+w,y,z),xyz(x+w,y+h,z),xyz(x,y+h,z),xyz(x,y,z+d),xyz(x+w,y,z+d),xyz(x+w,y+h,z+d),xyz(x,y+h,z+d)};
        for(int[] f:new int[][]{{0,1,2,3},{4,7,6,5},{0,4,5,1},{3,2,6,7},{0,3,7,4},{1,5,6,2}})quad(l,p[f[0]],p[f[1]],p[f[2]],p[f[3]]);
    }
    static float[] rays(int width,float length){
        Random random=new Random(44881);float[] data=new float[QUERIES*8];
        for(int i=0;i<QUERIES;i++){
            float x=(random.nextFloat()-.5f)*(width-4),z=(random.nextFloat()-.5f)*(width-4),y=height((int)Math.floor(x),(int)Math.floor(z))+random.nextFloat()*20+.1f;
            float dx=random.nextFloat()*2-1,dy=random.nextFloat()*2-1,dz=random.nextFloat()*2-1;float norm=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
            int k=i*8;data[k]=x;data[k+1]=y;data[k+2]=z;data[k+4]=dx/norm;data[k+5]=dy/norm;data[k+6]=dz/norm;data[k+7]=length;
        }return data;
    }
    static float[] curvedRays(){
        // Replay unoccluded Schwarzschild paths from a hovering camera. Not an end-to-end renderer:
        // later segments are still queried even when an earlier segment would have hit terrain.
        float[] data=new float[QUERIES*8];int count=0,path=0;
        while(count<QUERIES){int pixel=(path*1009)&16383,ix=pixel%128,iy=pixel/128;path++;double dx=(ix+.5-64)/80,dz=-1,dy=(iy+.5-64)/80;
            double norm=Math.sqrt(dx*dx+dy*dy+dz*dz);dx/=norm;dy/=norm;dz/=norm;double tangent=Math.sqrt(dx*dx+dy*dy),tx=dx/tangent,ty=dy/tangent;
            double u=8.0/36,v=-dz*u*Math.sqrt(1-u)/tangent,phi=0;double[] previous={0,12,36};
            for(int step=0;step<400&&count<QUERIES;step++){
                double h=.02,a=1.5*u*u-u,bu=v+h*a/2,bv=1.5*Math.pow(u+h*v/2,2)-(u+h*v/2),cu=v+h*bv/2,cv=1.5*Math.pow(u+h*bu/2,2)-(u+h*bu/2),du=v+h*cv,dv=1.5*Math.pow(u+h*cu,2)-(u+h*cu);
                double nextU=u+h*(v+2*bu+2*cu+du)/6,nextV=v+h*(a+2*bv+2*cv+dv)/6;phi+=h;
                if(nextU<=0||nextU>=1)break;
                double radius=8/nextU;double[] next={radius*Math.sin(phi)*tx,12+radius*Math.sin(phi)*ty,radius*Math.cos(phi)};
                double cx=next[0]-previous[0],cy=next[1]-previous[1],cz=next[2]-previous[2],distance=Math.sqrt(cx*cx+cy*cy+cz*cz);int split=Math.max(1,(int)Math.ceil(distance/16));
                for(int part=0;part<split&&count<QUERIES;part++) {int k=count++*8;data[k]=(float)(previous[0]+cx*part/split);data[k+1]=(float)(previous[1]+cy*part/split);data[k+2]=(float)(previous[2]+cz*part/split);data[k+4]=(float)(cx/distance);data[k+5]=(float)(cy/distance);data[k+6]=(float)(cz/distance);data[k+7]=(float)(distance/split);}
                previous=next;u=nextU;v=nextV;if(radius>120)break;
            }
        }return data;
    }
    static double cpuHit(Triangle[] triangles,float[] data,int ray){
        int k=ray*8;double ox=data[k],oy=data[k+1],oz=data[k+2],dx=data[k+4],dy=data[k+5],dz=data[k+6],best=data[k+7];boolean found=false;
        for(Triangle triangle:triangles){float[] p=triangle.p;double ax=p[3]-p[0],ay=p[4]-p[1],az=p[5]-p[2],bx=p[6]-p[0],by=p[7]-p[1],bz=p[8]-p[2];double px=dy*bz-dz*by,py=dz*bx-dx*bz,pz=dx*by-dy*bx,det=ax*px+ay*py+az*pz;if(Math.abs(det)<1e-7)continue;double tx=ox-p[0],ty=oy-p[1],tz=oz-p[2],u=(tx*px+ty*py+tz*pz)/det;if(u<0||u>1)continue;double qx=ty*az-tz*ay,qy=tz*ax-tx*az,qz=tx*ay-ty*ax,v=(dx*qx+dy*qy+dz*qz)/det;if(v<0||u+v>1)continue;double t=(bx*qx+by*qy+bz*qz)/det;if(t>=.0001&&t<best){best=t;found=true;}}
        return found?best:-1;
    }
    static boolean atTriangleEdge(Triangle[] triangles,float[] data,int ray,double t){
        // A classification disagreement is acceptable evidence only when the putative hit lies
        // within 1e-5 world units of a plane and 1e-5 barycentric units of an edge. The hardware
        // result must independently agree with the brute-force double-precision reference too.
        int k=ray*8;double x=data[k]+t*data[k+4],y=data[k+1]+t*data[k+5],z=data[k+2]+t*data[k+6];
        for(Triangle triangle:triangles){float[] p=triangle.p;double ax=p[3]-p[0],ay=p[4]-p[1],az=p[5]-p[2],bx=p[6]-p[0],by=p[7]-p[1],bz=p[8]-p[2],qx=x-p[0],qy=y-p[1],qz=z-p[2];
            double nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,nn=Math.sqrt(nx*nx+ny*ny+nz*nz);if(nn==0||Math.abs(nx*qx+ny*qy+nz*qz)>1e-5*nn)continue;
            double aa=ax*ax+ay*ay+az*az,bb=bx*bx+by*by+bz*bz,ab=ax*bx+ay*by+az*bz,qa=qx*ax+qy*ay+qz*az,qb=qx*bx+qy*by+qz*bz,det=aa*bb-ab*ab,u=(qa*bb-qb*ab)/det,v=(qb*aa-qa*ab)/det,w=1-u-v;
            if(Math.min(Math.min(u,v),w)>=-1e-5&&Math.max(Math.max(u,v),w)<=1+1e-5&&Math.min(Math.min(Math.abs(u),Math.abs(v)),Math.abs(w))<=1e-5)return true;
        }return false;
    }
    static double percentile(double[] a,double p){double[] copy=a.clone();Arrays.sort(copy);return copy[Math.min(copy.length-1,(int)Math.ceil(copy.length*p)-1)];}
    void run(int width) throws Exception {
        long start=System.nanoTime();Triangle[] triangles=scene(width);List<Node> nodes=new ArrayList<>();tree(triangles,0,triangles.length,nodes);double cpuMs=(System.nanoTime()-start)/1e6;
        Buffer vertices=buffer(triangles.length*48L,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT|VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR,true),bounds=buffer(nodes.size()*32L,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,true);
        for(Triangle t:triangles)for(int i=0;i<3;i++)vertices.mapped.putFloat(t.p[i*3]).putFloat(t.p[i*3+1]).putFloat(t.p[i*3+2]).putFloat(0);
        for(Node n:nodes){for(float value:n.lo)bounds.mapped.putFloat(value);bounds.mapped.putInt(n.escape);for(float value:n.hi)bounds.mapped.putFloat(value);bounds.mapped.putInt(n.count==0?0:n.first<<4|n.count);}
        start=System.nanoTime();Acceleration bottom=build(vertices,triangles.length,null),top=build(null,0,bottom);double wallBuild=(System.nanoTime()-start)/1e6;
        System.out.printf(Locale.ROOT,"SCENE width=%d triangles=%d nodes=%d cpuSceneAndTreeMs=%.3f blasGpuMs=%.3f tlasGpuMs=%.3f asAllocationBuildWallMs=%.3f asBytes=%d%n",width,triangles.length,nodes.size(),cpuMs,bottom.gpuMs,top.gpuMs,wallBuild,bottom.bytes+top.bytes);
        Buffer queries=buffer(QUERIES*32L,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,true),result=buffer(QUERIES*4L,VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,true);descriptors(top,vertices,bounds,queries,result);
        for(String workload:new String[]{"short4","long16","curvedReplay"}){
            float[] inputs=workload.equals("curvedReplay")?curvedRays():rays(width,workload.equals("short4")?4:16);queries.mapped.asFloatBuffer().put(inputs);
            dispatch(software,QUERIES);float[] sw=new float[QUERIES];result.mapped.asFloatBuffer().get(sw);dispatch(hardware,QUERIES);float[] hw=new float[QUERIES];result.mapped.asFloatBuffer().get(hw);
            int misses=0,edgeCases=0,distanceFailures=0,hits=0,cpuFailures=0;double maxError=0;
            for(int i=0;i<QUERIES;i++){if(sw[i]>=0)hits++;if((sw[i]<0)!=(hw[i]<0)){double cpu=cpuHit(triangles,inputs,i);boolean cpuMatches=(cpu<0)==(hw[i]<0)&&(cpu<0||Math.abs(cpu-hw[i])<=1e-4+2e-5*Math.abs(cpu));boolean edge=cpuMatches&&atTriangleEdge(triangles,inputs,i,Math.max(sw[i],hw[i]));if(edge)edgeCases++;else misses++;System.out.println("DISAGREEMENT ray="+i+" sw="+sw[i]+" hw="+hw[i]+" cpu="+cpu+" verifiedEdge="+edge);continue;}if(sw[i]>=0){double error=Math.abs(sw[i]-hw[i]);maxError=Math.max(error,maxError);if(error>1e-4+2e-5*Math.abs(sw[i]))distanceFailures++;}}
            for(int i=0;i<128;i++){int query=(i*2029+73)%QUERIES;double cpu=cpuHit(triangles,inputs,query);if((cpu<0)!=(hw[query]<0)||cpu>=0&&Math.abs(cpu-hw[query])>1e-4+2e-5*Math.abs(cpu))cpuFailures++;}
            System.out.printf(Locale.ROOT,"CHECK width=%d workload=%s queries=%d hits=%d classificationFailures=%d verifiedEdgeCases=%d distanceFailures=%d cpuFailures=%d maxDistanceError=%.9g%n",width,workload,QUERIES,hits,misses,edgeCases,distanceFailures,cpuFailures,maxError);
            if(misses+distanceFailures+cpuFailures!=0)throw new IllegalStateException("Query validation failed; timings suppressed");
            for(int i=0;i<WARMUP;i++){dispatch(software,QUERIES);dispatch(hardware,QUERIES);}
            double[] softwareMs=new double[ROUNDS],hardwareMs=new double[ROUNDS];
            for(int i=0;i<ROUNDS;i++){if(i%2==0){softwareMs[i]=dispatch(software,QUERIES);hardwareMs[i]=dispatch(hardware,QUERIES);}else{hardwareMs[i]=dispatch(hardware,QUERIES);softwareMs[i]=dispatch(software,QUERIES);}}
            String line=String.format(Locale.ROOT,"{\"width\":%d,\"triangles\":%d,\"workload\":\"%s\",\"queries\":%d,\"hits\":%d,\"softwareMedianMs\":%.6f,\"hardwareMedianMs\":%.6f,\"softwareP95Ms\":%.6f,\"hardwareP95Ms\":%.6f,\"softwareSamplesMs\":%s,\"hardwareSamplesMs\":%s}%n",width,triangles.length,workload,QUERIES,hits,percentile(softwareMs,.5),percentile(hardwareMs,.5),percentile(softwareMs,.95),percentile(hardwareMs,.95),Arrays.toString(softwareMs),Arrays.toString(hardwareMs));
            Files.writeString(Path.of("run/rtx/results.jsonl"),line,StandardOpenOption.CREATE,StandardOpenOption.APPEND);System.out.printf(Locale.ROOT,"TIMING width=%d workload=%s softwareMs=%.3f hardwareMs=%.3f speedup=%.2f%n",width,workload,percentile(softwareMs,.5),percentile(hardwareMs,.5),percentile(softwareMs,.5)/percentile(hardwareMs,.5));
        }
    }
    @Override public void close(){
        if(device!=null){vkDeviceWaitIdle(device);vkDestroyPipeline(device,hardware,null);vkDestroyPipeline(device,software,null);vkDestroyDescriptorPool(device,descriptorPool,null);vkDestroyPipelineLayout(device,layout,null);vkDestroyDescriptorSetLayout(device,descriptorLayout,null);for(int i=cleanup.size()-1;i>=0;i--)cleanup.get(i).run();vkDestroyQueryPool(device,queryPool,null);vkDestroyCommandPool(device,pool,null);vkDestroyDevice(device,null);}if(memory!=null)memory.free();if(instance!=null)vkDestroyInstance(instance,null);
    }
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);Files.createDirectories(Path.of("run/rtx"));Files.writeString(Path.of("run/rtx/results.jsonl"),"");
        for(int width:new int[]{96,192})try(Probe probe=new Probe()){probe.run(width);}
    }
}
