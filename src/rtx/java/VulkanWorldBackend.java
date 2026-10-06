import io.github.rohrl.interstellar.client.WorldRenderBackend;
import org.lwjgl.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.lwjgl.system.windows.Kernel32;
import org.lwjgl.vulkan.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.*;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.*;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.*;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.*;
import static org.lwjgl.opengl.EXTSemaphore.*;
import static org.lwjgl.opengl.EXTSemaphoreWin32.*;

/** Optional live backend: resident chunk geometry and shared native appearance, with no CPU image readback. */
public final class VulkanWorldBackend implements WorldRenderBackend {
    private Probe vk;private FullImageShader source;private Probe.Buffer parameters;
    private final List<Runnable> cleanup=new ArrayList<>();
    private long layout,descriptorLayout,descriptorPool,set,toVk,toGl;
    private record Pipelines(long initial,long material) {}
    private final EnumMap<Optics,Pipelines> pipelines=new EnumMap<>(Optics.class);
    private Optics optics=Optics.EXTERIOR;
    private int glToVk,glToGl,glMemory,sharedTexture;
    private TextureImage output;private final int width,height,sampleCapacity;
    private boolean closed,profile,rendered;private double previousGpu=Double.NaN;
    private LiveGeometry geometry;private long boundTop,boundTerrain;
    private Map<String,Image> frameImages;
    private record SharedInput(TextureImage image,int glMemory,int glTexture,Image format) {}
    private final Map<String,SharedInput> inputs=new LinkedHashMap<>();
    private record TextureImage(long image,long memory,long view,long sampler,long bytes) {}
    public VulkanWorldBackend(Scene scene) throws Exception {
        width=scene.width();height=scene.height();sampleCapacity=Math.max(2,scene.samples());frameImages=scene.images();
        try {
            var caps=GL.getCapabilities();
            if(!(caps.OpenGL43||caps.GL_ARB_copy_image)||!caps.GL_EXT_memory_object_win32||!caps.GL_EXT_semaphore_win32)throw new IllegalStateException("Windows Vulkan/OpenGL sharing unavailable");
            source=new FullImageShader(scene.opticalSource(),true);
            try(MemoryStack s=MemoryStack.stackPush()) {var uuid=s.malloc(16);EXTMemoryObject.glGetUnsignedBytei_vEXT(EXTMemoryObject.GL_DEVICE_UUID_EXT,0,uuid);vk=new Probe(null,0,0,48,false,uuid);}
            geometry=new LiveGeometry(vk,scene.terrain());geometry.update(new float[0],0,Long.MIN_VALUE);
            parameters=vk.buffer(128*16,VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,true);
            Map<String,TextureImage> textures=new LinkedHashMap<>();
            for(var entry:scene.images().entrySet()){var shared=sharedInput(entry.getValue());inputs.put(entry.getKey(),shared);textures.put(entry.getKey(),shared.image());}
            output=image(width*2,height*(sampleCapacity/2),VK_FORMAT_R32G32B32A32_SFLOAT,VK_IMAGE_USAGE_STORAGE_BIT|VK_IMAGE_USAGE_SAMPLED_BIT,true);
            importOutput();
            createDescriptors(textures);
            // All variants share descriptors/geometry. Prepare them during initial
            // loading so passing a source or horizon never compiles on that frame.
            for(var model:Optics.values()) {
                var variant=model==Optics.EXTERIOR?source:new FullImageShader(scene.opticalSource(),true,model);
                if(!variant.uniforms.equals(source.uniforms) || !variant.samplers.equals(source.samplers))
                    throw new IllegalStateException("RTX optical variants must share bindings");
                long initial=pipeline(variant.probe,model+"-initial");
                pipelines.put(model,new Pipelines(initial,0));
                pipelines.put(model,new Pipelines(initial,pipeline(variant.material,model+"-material")));
            }
            vk.begin();geometry.record();for(var input:inputs.values())barrier(input.image.image,VK_IMAGE_LAYOUT_UNDEFINED,VK_IMAGE_LAYOUT_GENERAL,vk.queueFamily,VK_QUEUE_FAMILY_EXTERNAL,0,0);barrier(output.image,VK_IMAGE_LAYOUT_UNDEFINED,VK_IMAGE_LAYOUT_GENERAL,vk.queueFamily,VK_QUEUE_FAMILY_EXTERNAL,0,0);vk.finish();
            verifyGeometry();
            long[] sem=semaphore();toVk=sem[0];glToVk=(int)sem[1];sem=semaphore();toGl=sem[0];glToGl=(int)sem[1];
            glCheck();
            System.out.println("RTX live backend ready: "+width+"x"+height+" dynamic AA, resident chunks + moving geometry, GPU appearance sharing; optics="+pipelines.keySet());
        } catch(Exception|LinkageError failure) {close();throw failure;}
    }
    private static void check(int status){Probe.check(status);}
    private void verifyGeometry() throws Exception {
        if(geometry.checkRay==null)return;var r=geometry.checkRay;
        String code="#version 460\n#extension GL_EXT_ray_query : require\nlayout(local_size_x=1) in;layout(set=0,binding=0) uniform accelerationStructureEXT scene;layout(std430,set=0,binding=1) readonly buffer Vertices{vec4 vertices[];};layout(std430,set=0,binding="+source.movingBinding+") buffer Output{vec4 result[];};void main(){rayQueryEXT q;rayQueryInitializeEXT(q,scene,0u,1u,vec3("+r[0]+","+r[1]+","+r[2]+"),0.,vec3("+r[3]+","+r[4]+","+r[5]+"),1.);int hits=0;while(rayQueryProceedEXT(q)){hits++;rayQueryConfirmIntersectionEXT(q);}result[0]=vec4(hits,rayQueryGetIntersectionTypeEXT(q,true)==0u?-1.:rayQueryGetIntersectionTEXT(q,true),0,0);result[1]=vertices["+geometry.checkVertex+"]; }";
        long pipeline=pipeline(code,"geometry-check");
        try(MemoryStack s=MemoryStack.stackPush()) {
            vk.begin();vkCmdBindDescriptorSets(vk.command,VK_PIPELINE_BIND_POINT_COMPUTE,layout,0,s.longs(set),null);vkCmdBindPipeline(vk.command,VK_PIPELINE_BIND_POINT_COMPUTE,pipeline);vkCmdDispatch(vk.command,1,1,1);
            var barrier=VkMemoryBarrier.calloc(1,s).sType$Default().srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT).dstAccessMask(VK_ACCESS_HOST_READ_BIT);vkCmdPipelineBarrier(vk.command,VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,VK_PIPELINE_STAGE_HOST_BIT,0,barrier,null,null);vk.finish();
            float[] result=new float[8];geometry.moving.mapped().asFloatBuffer().get(result);System.out.println("RTX geometry check="+Arrays.toString(result));
            if(result[0]<1 || !Float.isFinite(result[1]) || result[1]<0 || result[1]>1)throw new IllegalStateException("RTX initialization ray missed captured terrain; retain OpenGL");
        }finally{vkDestroyPipeline(vk.device,pipeline,null);}
    }
    private static void glCheck(){int e=GL11.glGetError();if(e!=0)throw new IllegalStateException("RTX GL error 0x"+Integer.toHexString(e));}
    private static void closeHandle(long handle){if(JNI.callPI(handle,Kernel32.getLibrary().getFunctionAddress("CloseHandle"))==0)throw new IllegalStateException("CloseHandle failed");}
    private int memoryType(int bits){for(int i=0;i<vk.memory.memoryTypeCount();i++)if((bits&(1<<i))!=0&&(vk.memory.memoryTypes(i).propertyFlags()&VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)!=0)return i;throw new IllegalStateException("No GPU-local image memory");}
    private TextureImage image(int w,int h,int format,int usage,boolean shared) {
        try(MemoryStack s=MemoryStack.stackPush()) {
            var out=s.mallocLong(1);var external=VkExternalMemoryImageCreateInfo.calloc(s).sType$Default().handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);
            var info=VkImageCreateInfo.calloc(s).sType$Default().imageType(VK_IMAGE_TYPE_2D).format(format).mipLevels(1).arrayLayers(1).samples(VK_SAMPLE_COUNT_1_BIT).tiling(VK_IMAGE_TILING_OPTIMAL).usage(usage).sharingMode(VK_SHARING_MODE_EXCLUSIVE).initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);info.extent().set(w,h,1);
            if(shared) {
                info.pNext(external.address());var properties=VkExternalImageFormatProperties.calloc(s).sType$Default();
                var query=VkPhysicalDeviceExternalImageFormatInfo.calloc(s).sType$Default().handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);
                check(vkGetPhysicalDeviceImageFormatProperties2(vk.physical,VkPhysicalDeviceImageFormatInfo2.calloc(s).sType$Default().pNext(query.address()).format(format).type(info.imageType()).tiling(info.tiling()).usage(usage),VkImageFormatProperties2.calloc(s).sType$Default().pNext(properties.address())));
                if((properties.externalMemoryProperties().externalMemoryFeatures()&VK_EXTERNAL_MEMORY_FEATURE_EXPORTABLE_BIT)==0)throw new IllegalStateException("RGBA32F sharing unsupported");
            }
            check(vkCreateImage(vk.device,info,null,out));long image=out.get(0);
            long[] owned={image,0,0};cleanup.add(()->{if(owned[2]!=0)vkDestroyImageView(vk.device,owned[2],null);vkDestroyImage(vk.device,owned[0],null);if(owned[1]!=0)vkFreeMemory(vk.device,owned[1],null);});
            var requirements=VkMemoryRequirements.calloc(s);vkGetImageMemoryRequirements(vk.device,image,requirements);
            var export=VkExportMemoryAllocateInfo.calloc(s).sType$Default().handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);
            var dedicated=VkMemoryDedicatedAllocateInfo.calloc(s).sType$Default().image(image);if(shared)dedicated.pNext(export.address());
            check(vkAllocateMemory(vk.device,VkMemoryAllocateInfo.calloc(s).sType$Default().pNext(dedicated.address()).allocationSize(requirements.size()).memoryTypeIndex(memoryType(requirements.memoryTypeBits())),null,out));long memory=out.get(0);owned[1]=memory;check(vkBindImageMemory(vk.device,image,memory,0));
            var viewInfo=VkImageViewCreateInfo.calloc(s).sType$Default().image(image).viewType(VK_IMAGE_VIEW_TYPE_2D).format(format);viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(1).layerCount(1);
            check(vkCreateImageView(vk.device,viewInfo,null,out));long view=out.get(0);owned[2]=view;
            return new TextureImage(image,memory,view,0,requirements.size());
        }
    }
    private static int filter(int gl){return gl==GL11.GL_NEAREST||gl==GL11.GL_NEAREST_MIPMAP_NEAREST||gl==GL11.GL_NEAREST_MIPMAP_LINEAR?VK_FILTER_NEAREST:VK_FILTER_LINEAR;}
    private static int wrap(int gl){return gl==GL11.GL_REPEAT?VK_SAMPLER_ADDRESS_MODE_REPEAT:gl==GL14.GL_MIRRORED_REPEAT?VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT:VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;}
    private SharedInput sharedInput(Image format) {
        int glFormat=format.format();
        if(glFormat!=GL11.GL_RGBA8 && glFormat!=GL21.GL_SRGB8_ALPHA8)throw new IllegalArgumentException("RTX appearance requires RGBA8/sRGB8_ALPHA8 textures, got "+glFormat);
        TextureImage image=image(format.width(),format.height(),glFormat==GL11.GL_RGBA8?VK_FORMAT_R8G8B8A8_UNORM:VK_FORMAT_R8G8B8A8_SRGB,VK_IMAGE_USAGE_TRANSFER_DST_BIT|VK_IMAGE_USAGE_SAMPLED_BIT,true);
        try(MemoryStack s=MemoryStack.stackPush()) {
            var out=s.mallocLong(1);check(vkCreateSampler(vk.device,VkSamplerCreateInfo.calloc(s).sType$Default().minFilter(filter(format.minFilter())).magFilter(filter(format.magFilter())).mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST).addressModeU(wrap(format.wrapS())).addressModeV(wrap(format.wrapT())).addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE).maxLod(0),null,out));long sampler=out.get(0);cleanup.add(()->vkDestroySampler(vk.device,sampler,null));
            var nativeHandle=s.mallocPointer(1);check(vkGetMemoryWin32HandleKHR(vk.device,VkMemoryGetWin32HandleInfoKHR.calloc(s).sType$Default().memory(image.memory).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT),nativeHandle));
            int memory=glCreateMemoryObjectsEXT();cleanup.add(()->glDeleteMemoryObjectsEXT(memory));glMemoryObjectParameteriEXT(memory,GL_DEDICATED_MEMORY_OBJECT_EXT,GL11.GL_TRUE);
            try{glImportMemoryWin32HandleEXT(memory,image.bytes,EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT,nativeHandle.get(0));}finally{closeHandle(nativeHandle.get(0));}
            int texture=GL11.glGenTextures();cleanup.add(()->GL11.glDeleteTextures(texture));GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL_TEXTURE_TILING_EXT,GL_OPTIMAL_TILING_EXT);
            glTexStorageMem2DEXT(GL11.GL_TEXTURE_2D,1,glFormat,format.width(),format.height(),memory,0);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            return new SharedInput(new TextureImage(image.image,image.memory,image.view,sampler,image.bytes),memory,texture,format);
        }
    }
    private void barrier(long image,int before,int after,int sourceFamily,int destinationFamily,int sourceAccess,int destinationAccess) {try(MemoryStack s=MemoryStack.stackPush()) {
        var b=VkImageMemoryBarrier.calloc(1,s).sType$Default().oldLayout(before).newLayout(after).srcQueueFamilyIndex(sourceFamily).dstQueueFamilyIndex(destinationFamily).srcAccessMask(sourceAccess).dstAccessMask(destinationAccess).image(image);b.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(1).layerCount(1);
        vkCmdPipelineBarrier(vk.command,VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,0,null,null,b);
    }}
    private void importOutput() {try(MemoryStack s=MemoryStack.stackPush()) {
        var handle=s.mallocPointer(1);check(vkGetMemoryWin32HandleKHR(vk.device,VkMemoryGetWin32HandleInfoKHR.calloc(s).sType$Default().memory(output.memory).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT),handle));
        glMemory=glCreateMemoryObjectsEXT();glMemoryObjectParameteriEXT(glMemory,GL_DEDICATED_MEMORY_OBJECT_EXT,GL11.GL_TRUE);
        try {glImportMemoryWin32HandleEXT(glMemory,output.bytes,EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT,handle.get(0));}finally{closeHandle(handle.get(0));}
        sharedTexture=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,sharedTexture);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL_TEXTURE_TILING_EXT,GL_OPTIMAL_TILING_EXT);glTexStorageMem2DEXT(GL11.GL_TEXTURE_2D,1,GL30.GL_RGBA32F,width*2,height*(sampleCapacity/2),glMemory,0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
    }}
    private long[] semaphore(){try(MemoryStack s=MemoryStack.stackPush()) {
        var out=s.mallocLong(1);var export=VkExportSemaphoreCreateInfo.calloc(s).sType$Default().handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT);
        check(vkCreateSemaphore(vk.device,VkSemaphoreCreateInfo.calloc(s).sType$Default().pNext(export.address()),null,out));long handle=out.get(0);cleanup.add(()->vkDestroySemaphore(vk.device,handle,null));int gl=glGenSemaphoresEXT();cleanup.add(()->glDeleteSemaphoresEXT(gl));
        var nativeHandle=s.mallocPointer(1);check(vkGetSemaphoreWin32HandleKHR(vk.device,VkSemaphoreGetWin32HandleInfoKHR.calloc(s).sType$Default().semaphore(handle).handleType(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT),nativeHandle));
        try {glImportSemaphoreWin32HandleEXT(gl,EXTSemaphoreWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT,nativeHandle.get(0));}finally{closeHandle(nativeHandle.get(0));}
        return new long[]{handle,gl};
    }}
    private void createDescriptors(Map<String,TextureImage> textures) {try(MemoryStack s=MemoryStack.stackPush()) {
        int count=5+source.samplers.size();var bindings=VkDescriptorSetLayoutBinding.calloc(count,s);int[] types={VK_DESCRIPTOR_TYPE_ACCELERATION_STRUCTURE_KHR,VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,VK_DESCRIPTOR_TYPE_STORAGE_IMAGE};
        for(int i=0;i<count;i++)bindings.get(i).binding(i).descriptorCount(1).descriptorType(i<4?types[i]:i==source.movingBinding?VK_DESCRIPTOR_TYPE_STORAGE_BUFFER:VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
        var out=s.mallocLong(1);check(vkCreateDescriptorSetLayout(vk.device,VkDescriptorSetLayoutCreateInfo.calloc(s).sType$Default().pBindings(bindings),null,out));descriptorLayout=out.get(0);
        check(vkCreatePipelineLayout(vk.device,VkPipelineLayoutCreateInfo.calloc(s).sType$Default().pSetLayouts(s.longs(descriptorLayout)),null,out));layout=out.get(0);
        var sizes=VkDescriptorPoolSize.calloc(5,s);for(int i=0;i<4;i++)sizes.get(i).type(types[i]).descriptorCount(i==1?2:1);sizes.get(4).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(source.samplers.size());
        check(vkCreateDescriptorPool(vk.device,VkDescriptorPoolCreateInfo.calloc(s).sType$Default().maxSets(1).pPoolSizes(sizes),null,out));descriptorPool=out.get(0);
        check(vkAllocateDescriptorSets(vk.device,VkDescriptorSetAllocateInfo.calloc(s).sType$Default().descriptorPool(descriptorPool).pSetLayouts(s.longs(descriptorLayout)),out));set=out.get(0);
        var writes=VkWriteDescriptorSet.calloc(count,s);for(int i=0;i<count;i++)writes.get(i).sType$Default().dstSet(set).dstBinding(i).descriptorCount(1).descriptorType(i<4?types[i]:i==source.movingBinding?VK_DESCRIPTOR_TYPE_STORAGE_BUFFER:VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
        writes.get(0).pNext(VkWriteDescriptorSetAccelerationStructureKHR.calloc(s).sType$Default().pAccelerationStructures(s.longs(geometry.top.handle)).address());
        writes.get(1).pBufferInfo(VkDescriptorBufferInfo.calloc(1,s).buffer(geometry.terrain.handle()).range(geometry.terrain.size()));writes.get(2).pBufferInfo(VkDescriptorBufferInfo.calloc(1,s).buffer(parameters.handle()).range(parameters.size()));
        writes.get(source.movingBinding).pBufferInfo(VkDescriptorBufferInfo.calloc(1,s).buffer(geometry.moving.handle()).range(geometry.moving.size()));
        writes.get(3).pImageInfo(VkDescriptorImageInfo.calloc(1,s).imageView(output.view).imageLayout(VK_IMAGE_LAYOUT_GENERAL));
        for(var entry:source.samplers.entrySet()) {
            TextureImage image=textures.getOrDefault(entry.getKey(),textures.get("Atlas"));
            writes.get(entry.getValue()).pImageInfo(VkDescriptorImageInfo.calloc(1,s).imageView(image.view).sampler(image.sampler).imageLayout(VK_IMAGE_LAYOUT_GENERAL));
        }
        vkUpdateDescriptorSets(vk.device,writes,null);boundTop=geometry.top.handle;boundTerrain=geometry.terrain.handle();
    }}
    private long pipeline(String code,String name) throws Exception {
        long compiler=shaderc_compiler_initialize(),options=shaderc_compile_options_initialize();
        shaderc_compile_options_set_target_env(options,shaderc_target_env_vulkan,shaderc_env_version_vulkan_1_2);shaderc_compile_options_set_optimization_level(options,shaderc_optimization_level_performance);
        long result=RtxShaderCompiler.compile(compiler,code,shaderc_compute_shader,name,"main",options);
        try(MemoryStack s=MemoryStack.stackPush()) {
            if(shaderc_result_get_compilation_status(result)!=shaderc_compilation_status_success)throw new IllegalStateException(shaderc_result_get_error_message(result));
            var out=s.mallocLong(1);check(vkCreateShaderModule(vk.device,VkShaderModuleCreateInfo.calloc(s).sType$Default().pCode(shaderc_result_get_bytes(result)),null,out));long module=out.get(0);
            try {var stage=VkPipelineShaderStageCreateInfo.calloc(s).sType$Default().stage(VK_SHADER_STAGE_COMPUTE_BIT).module(module).pName(s.UTF8("main"));
                check(vkCreateComputePipelines(vk.device,VK_NULL_HANDLE,VkComputePipelineCreateInfo.calloc(1,s).sType$Default().stage(stage).layout(layout),null,out));return out.get(0);
            }finally{vkDestroyShaderModule(vk.device,module,null);}
        }finally{shaderc_result_release(result);shaderc_compile_options_release(options);shaderc_compiler_release(compiler);}
    }
    @Override public void update(float[] triangles,int count,long revision,Map<String,Image> images) {
        long waitStart=io.github.rohrl.interstellar.client.RefreshProfile.start();check(vkQueueWaitIdle(vk.queue));io.github.rohrl.interstellar.client.RefreshProfile.end(io.github.rohrl.interstellar.client.RefreshProfile.WAIT,waitStart);
        if(profile && rendered)previousGpu=completedGpuMillis();
        geometry.update(triangles,count,revision);frameImages=images;
        for(var entry:inputs.entrySet()) {
            var before=entry.getValue().format;var next=images.get(entry.getKey());
            if(next==null || before.width()!=next.width() || before.height()!=next.height() || before.format()!=next.format() || before.minFilter()!=next.minFilter() || before.magFilter()!=next.magFilter() || before.wrapS()!=next.wrapS() || before.wrapT()!=next.wrapT())throw new IllegalStateException("RTX appearance layout changed; reinitialize backend");
        }
        if(boundTop!=geometry.top.handle)try(MemoryStack s=MemoryStack.stackPush()) {
            var write=VkWriteDescriptorSet.calloc(1,s).sType$Default().dstSet(set).dstBinding(0).descriptorCount(1).descriptorType(VK_DESCRIPTOR_TYPE_ACCELERATION_STRUCTURE_KHR)
                .pNext(VkWriteDescriptorSetAccelerationStructureKHR.calloc(s).sType$Default().pAccelerationStructures(s.longs(geometry.top.handle)).address());
            vkUpdateDescriptorSets(vk.device,write,null);boundTop=geometry.top.handle;
        }
        if(boundTerrain!=geometry.terrain.handle())try(MemoryStack s=MemoryStack.stackPush()) {
            var write=VkWriteDescriptorSet.calloc(1,s).sType$Default().dstSet(set).dstBinding(1).descriptorCount(1).descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                .pBufferInfo(VkDescriptorBufferInfo.calloc(1,s).buffer(geometry.terrain.handle()).range(geometry.terrain.size()));
            vkUpdateDescriptorSets(vk.device,write,null);boundTerrain=geometry.terrain.handle();
        }
    }
    @Override public int render(Map<String,float[]> values) {
        if(closed)throw new IllegalStateException("Closed RTX backend");
        int requested=values.containsKey("RaySamples")?(int)values.get("RaySamples")[0]:2;
        int sampleCount=requested==1?1:requested==4?4:requested==8?8:2;
        if(sampleCount>sampleCapacity)throw new IllegalStateException("RTX AA target needs resizing");
        for(var entry:source.uniforms.entrySet()) {float[] value=values.get(entry.getKey());for(int i=0;i<4;i++)parameters.mapped().putFloat(entry.getValue()*16+i*4,value==null?0:value[i]);}
        try(MemoryStack s=MemoryStack.stackPush()) {
            var textures=s.mallocInt(inputs.size()+1);var layouts=s.mallocInt(inputs.size()+1);textures.put(sharedTexture);layouts.put(GL_LAYOUT_GENERAL_EXT);
            for(var entry:inputs.entrySet()) {
                var input=entry.getValue();var current=frameImages.get(entry.getKey());
                ARBCopyImage.glCopyImageSubData(current.id(),GL11.GL_TEXTURE_2D,0,0,0,0,input.glTexture,GL11.GL_TEXTURE_2D,0,0,0,0,current.width(),current.height(),1);
                textures.put(input.glTexture);layouts.put(GL_LAYOUT_GENERAL_EXT);
            }
            textures.flip();layouts.flip();
            if(!rendered)glCheck();
            glSignalSemaphoreEXT(glToVk,s.mallocInt(0),textures,layouts);GL11.glFlush();
            vk.begin();geometry.record();
            barrier(output.image,VK_IMAGE_LAYOUT_GENERAL,VK_IMAGE_LAYOUT_GENERAL,VK_QUEUE_FAMILY_EXTERNAL,vk.queueFamily,0,VK_ACCESS_SHADER_WRITE_BIT);
            for(var input:inputs.values())barrier(input.image.image,VK_IMAGE_LAYOUT_GENERAL,VK_IMAGE_LAYOUT_GENERAL,VK_QUEUE_FAMILY_EXTERNAL,vk.queueFamily,0,VK_ACCESS_SHADER_READ_BIT);
            vkCmdBindDescriptorSets(vk.command,VK_PIPELINE_BIND_POINT_COMPUTE,layout,0,s.longs(set),null);
            var programs=pipelines.get(optics);
            vkCmdBindPipeline(vk.command,VK_PIPELINE_BIND_POINT_COMPUTE,programs.initial);vkCmdDispatch(vk.command,(width*Math.min(sampleCount,2)+7)/8,(height*Math.max(sampleCount/2,1)+7)/8,1);
            barrier(output.image,VK_IMAGE_LAYOUT_GENERAL,VK_IMAGE_LAYOUT_GENERAL,vk.queueFamily,vk.queueFamily,VK_ACCESS_SHADER_WRITE_BIT,VK_ACCESS_SHADER_READ_BIT|VK_ACCESS_SHADER_WRITE_BIT);
            vkCmdBindPipeline(vk.command,VK_PIPELINE_BIND_POINT_COMPUTE,programs.material);vkCmdDispatch(vk.command,(width*Math.min(sampleCount,2)+7)/8,(height*Math.max(sampleCount/2,1)+7)/8,1);
            barrier(output.image,VK_IMAGE_LAYOUT_GENERAL,VK_IMAGE_LAYOUT_GENERAL,vk.queueFamily,VK_QUEUE_FAMILY_EXTERNAL,VK_ACCESS_SHADER_WRITE_BIT,0);
            for(var input:inputs.values())barrier(input.image.image,VK_IMAGE_LAYOUT_GENERAL,VK_IMAGE_LAYOUT_GENERAL,vk.queueFamily,VK_QUEUE_FAMILY_EXTERNAL,VK_ACCESS_SHADER_READ_BIT,0);
            vkCmdWriteTimestamp(vk.command,VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,vk.queryPool,1);check(vkEndCommandBuffer(vk.command));
            var submit=VkSubmitInfo.calloc(s).sType$Default().pCommandBuffers(s.pointers(vk.command.address())).pWaitSemaphores(s.longs(toVk)).pWaitDstStageMask(s.ints(VK_PIPELINE_STAGE_ALL_COMMANDS_BIT)).pSignalSemaphores(s.longs(toGl));check(vkQueueSubmit(vk.queue,submit,VK_NULL_HANDLE));
            glWaitSemaphoreEXT(glToGl,s.mallocInt(0),textures,layouts);
        }
        rendered=true;return sharedTexture;
    }
    @Override public void profiling(boolean enabled){profile=enabled;}
    @Override public void optics(Optics model) {
        if(!pipelines.containsKey(model))throw new IllegalArgumentException("Unknown RTX optics: "+model);
        if(optics!=model)System.out.println("RTX optics: "+model+" (resident geometry retained)");
        optics=model;
    }
    @Override public double previousGpuMillis(){return previousGpu;}
    @Override public String description(){return "RTX live world | shared native appearance + selective materials | "+width+"x"+height;}
    @Override public double completedGpuMillis() {
        check(vkQueueWaitIdle(vk.queue));
        try(MemoryStack s=MemoryStack.stackPush()) {
            var timestamps=s.mallocLong(2);
            check(vkGetQueryPoolResults(vk.device,vk.queryPool,0,2,timestamps,8,VK_QUERY_RESULT_64_BIT|VK_QUERY_RESULT_WAIT_BIT));
            return (timestamps.get(1)-timestamps.get(0))*vk.timestampPeriod/1e6;
        }
    }
    @Override public void close() {
        if(closed)return;closed=true;GL11.glFinish();if(vk==null)return;vkDeviceWaitIdle(vk.device);
        if(sharedTexture!=0)GL11.glDeleteTextures(sharedTexture);if(glMemory!=0)glDeleteMemoryObjectsEXT(glMemory);
        for(var p:pipelines.values()){vkDestroyPipeline(vk.device,p.initial,null);vkDestroyPipeline(vk.device,p.material,null);}
        vkDestroyDescriptorPool(vk.device,descriptorPool,null);vkDestroyPipelineLayout(vk.device,layout,null);vkDestroyDescriptorSetLayout(vk.device,descriptorLayout,null);
        for(int i=cleanup.size()-1;i>=0;i--)cleanup.get(i).run();if(geometry!=null)geometry.close();vk.close();
    }
}
