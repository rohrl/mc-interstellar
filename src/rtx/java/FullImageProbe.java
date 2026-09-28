import io.github.rohrl.interstellar.client.FrozenWorldBackend;
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

/** Frozen full-image experiment. Uses the standalone probe's allocation/AS helpers;
 * shipped only by the explicit experimental build, behind the Vulkan-free backend interface. */
public final class FullImageProbe implements FrozenWorldBackend {
    private Probe vk;private FullImageShader source;private Probe.Buffer parameters;
    private final List<Runnable> cleanup=new ArrayList<>();
    private long layout,descriptorLayout,descriptorPool,set,initialPipeline,materialPipeline,toVk,toGl;
    private int glToVk,glToGl,glMemory,sharedTexture;
    private TextureImage output;private final int width,height;
    private final Path directory;private boolean closed;
    private record TextureImage(long image,long memory,long view,long sampler,long bytes) {}
    public FullImageProbe(Scene scene) throws Exception {
        width=scene.width();height=scene.height();directory=scene.geometry();
        try {
            var caps=GL.getCapabilities();
            if(!caps.GL_EXT_memory_object_win32||!caps.GL_EXT_semaphore_win32)throw new IllegalStateException("Windows Vulkan/OpenGL sharing unavailable");
            source=new FullImageShader(scene.opticalSource());
            Files.writeString(directory.resolve("initial.comp"),source.probe);Files.writeString(directory.resolve("material.comp"),source.material);
            try(MemoryStack s=MemoryStack.stackPush()) {var uuid=s.malloc(16);EXTMemoryObject.glGetUnsignedBytei_vEXT(EXTMemoryObject.GL_DEVICE_UUID_EXT,0,uuid);vk=new Probe(null,0,0,48,false,uuid);}
            var geometry=new NativeReplay(directory);geometry.upload(vk);
            parameters=vk.buffer(128*16,VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,true);
            Map<String,TextureImage> textures=new LinkedHashMap<>();
            for(var entry:scene.textures().entrySet())textures.put(entry.getKey(),uploadTexture(entry.getValue()));
            output=image(width*2,height,VK_FORMAT_R32G32B32A32_SFLOAT,VK_IMAGE_USAGE_STORAGE_BIT|VK_IMAGE_USAGE_SAMPLED_BIT,true);
            importOutput();
            createDescriptors(geometry.top,geometry.vertices,textures);
            initialPipeline=pipeline(source.probe,"initial");materialPipeline=pipeline(source.material,"material");
            vk.begin();barrier(output.image,VK_IMAGE_LAYOUT_UNDEFINED,VK_IMAGE_LAYOUT_GENERAL,vk.queueFamily,VK_QUEUE_FAMILY_EXTERNAL,0,0);vk.finish();
            long[] sem=semaphore();toVk=sem[0];glToVk=(int)sem[1];sem=semaphore();toGl=sem[0];glToGl=(int)sem[1];
            glCheck();
            System.out.println("RTX full-image ready: "+geometry.count+" triangles, "+width+"x"+height+" sharp2x; "+directory);
        } catch(Exception|LinkageError failure) {close();throw failure;}
    }
    private static void check(int status){Probe.check(status);}
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
            var requirements=VkMemoryRequirements.calloc(s);vkGetImageMemoryRequirements(vk.device,image,requirements);
            var export=VkExportMemoryAllocateInfo.calloc(s).sType$Default().handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);
            var dedicated=VkMemoryDedicatedAllocateInfo.calloc(s).sType$Default().image(image);if(shared)dedicated.pNext(export.address());
            check(vkAllocateMemory(vk.device,VkMemoryAllocateInfo.calloc(s).sType$Default().pNext(dedicated.address()).allocationSize(requirements.size()).memoryTypeIndex(memoryType(requirements.memoryTypeBits())),null,out));long memory=out.get(0);check(vkBindImageMemory(vk.device,image,memory,0));
            var viewInfo=VkImageViewCreateInfo.calloc(s).sType$Default().image(image).viewType(VK_IMAGE_VIEW_TYPE_2D).format(format);viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(1).layerCount(1);
            check(vkCreateImageView(vk.device,viewInfo,null,out));long view=out.get(0);
            cleanup.add(()->{vkDestroyImageView(vk.device,view,null);vkDestroyImage(vk.device,image,null);vkFreeMemory(vk.device,memory,null);});
            return new TextureImage(image,memory,view,0,requirements.size());
        }
    }
    private static int filter(int gl){return gl==GL11.GL_NEAREST||gl==GL11.GL_NEAREST_MIPMAP_NEAREST||gl==GL11.GL_NEAREST_MIPMAP_LINEAR?VK_FILTER_NEAREST:VK_FILTER_LINEAR;}
    private static int wrap(int gl){return gl==GL11.GL_REPEAT?VK_SAMPLER_ADDRESS_MODE_REPEAT:gl==GL14.GL_MIRRORED_REPEAT?VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT:VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;}
    private TextureImage uploadTexture(Texture pixels) {
        int format=pixels.format()==GL21.GL_SRGB8_ALPHA8||pixels.format()==GL21.GL_SRGB8?VK_FORMAT_R8G8B8A8_SRGB:VK_FORMAT_R8G8B8A8_UNORM;
        TextureImage image=image(pixels.width(),pixels.height(),format,VK_IMAGE_USAGE_TRANSFER_DST_BIT|VK_IMAGE_USAGE_SAMPLED_BIT,false);
        var upload=vk.buffer(pixels.rgba().remaining(),VK_BUFFER_USAGE_TRANSFER_SRC_BIT,true);upload.mapped().put(pixels.rgba().duplicate()).flip();
        try(MemoryStack s=MemoryStack.stackPush()) {
            var out=s.mallocLong(1);check(vkCreateSampler(vk.device,VkSamplerCreateInfo.calloc(s).sType$Default().minFilter(filter(pixels.minFilter())).magFilter(filter(pixels.magFilter())).mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST).addressModeU(wrap(pixels.wrapS())).addressModeV(wrap(pixels.wrapT())).addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE).maxLod(0),null,out));long sampler=out.get(0);cleanup.add(()->vkDestroySampler(vk.device,sampler,null));
            vk.begin();barrier(image.image,VK_IMAGE_LAYOUT_UNDEFINED,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,vk.queueFamily,vk.queueFamily,0,VK_ACCESS_TRANSFER_WRITE_BIT);
            var copy=VkBufferImageCopy.calloc(1,s);copy.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).layerCount(1);copy.imageExtent().set(pixels.width(),pixels.height(),1);
            vkCmdCopyBufferToImage(vk.command,upload.handle(),image.image,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,copy);
            barrier(image.image,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,vk.queueFamily,vk.queueFamily,VK_ACCESS_TRANSFER_WRITE_BIT,VK_ACCESS_SHADER_READ_BIT);vk.finish();
            return new TextureImage(image.image,image.memory,image.view,sampler,image.bytes);
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
        sharedTexture=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,sharedTexture);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL_TEXTURE_TILING_EXT,GL_OPTIMAL_TILING_EXT);glTexStorageMem2DEXT(GL11.GL_TEXTURE_2D,1,GL30.GL_RGBA32F,width*2,height,glMemory,0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
    }}
    private long[] semaphore(){try(MemoryStack s=MemoryStack.stackPush()) {
        var out=s.mallocLong(1);var export=VkExportSemaphoreCreateInfo.calloc(s).sType$Default().handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT);
        check(vkCreateSemaphore(vk.device,VkSemaphoreCreateInfo.calloc(s).sType$Default().pNext(export.address()),null,out));long handle=out.get(0);int gl=glGenSemaphoresEXT();
        var nativeHandle=s.mallocPointer(1);check(vkGetSemaphoreWin32HandleKHR(vk.device,VkSemaphoreGetWin32HandleInfoKHR.calloc(s).sType$Default().semaphore(handle).handleType(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT),nativeHandle));
        try {glImportSemaphoreWin32HandleEXT(gl,EXTSemaphoreWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT,nativeHandle.get(0));}finally{closeHandle(nativeHandle.get(0));}
        return new long[]{handle,gl};
    }}
    private void createDescriptors(Probe.Acceleration scene,Probe.Buffer vertices,Map<String,TextureImage> textures) {try(MemoryStack s=MemoryStack.stackPush()) {
        int count=4+source.samplers.size();var bindings=VkDescriptorSetLayoutBinding.calloc(count,s);int[] types={VK_DESCRIPTOR_TYPE_ACCELERATION_STRUCTURE_KHR,VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,VK_DESCRIPTOR_TYPE_STORAGE_IMAGE};
        for(int i=0;i<count;i++)bindings.get(i).binding(i).descriptorCount(1).descriptorType(i<4?types[i]:VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
        var out=s.mallocLong(1);check(vkCreateDescriptorSetLayout(vk.device,VkDescriptorSetLayoutCreateInfo.calloc(s).sType$Default().pBindings(bindings),null,out));descriptorLayout=out.get(0);
        check(vkCreatePipelineLayout(vk.device,VkPipelineLayoutCreateInfo.calloc(s).sType$Default().pSetLayouts(s.longs(descriptorLayout)),null,out));layout=out.get(0);
        var sizes=VkDescriptorPoolSize.calloc(5,s);for(int i=0;i<4;i++)sizes.get(i).type(types[i]).descriptorCount(1);sizes.get(4).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(source.samplers.size());
        check(vkCreateDescriptorPool(vk.device,VkDescriptorPoolCreateInfo.calloc(s).sType$Default().maxSets(1).pPoolSizes(sizes),null,out));descriptorPool=out.get(0);
        check(vkAllocateDescriptorSets(vk.device,VkDescriptorSetAllocateInfo.calloc(s).sType$Default().descriptorPool(descriptorPool).pSetLayouts(s.longs(descriptorLayout)),out));set=out.get(0);
        var writes=VkWriteDescriptorSet.calloc(count,s);for(int i=0;i<count;i++)writes.get(i).sType$Default().dstSet(set).dstBinding(i).descriptorCount(1).descriptorType(i<4?types[i]:VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
        writes.get(0).pNext(VkWriteDescriptorSetAccelerationStructureKHR.calloc(s).sType$Default().pAccelerationStructures(s.longs(scene.handle())).address());
        writes.get(1).pBufferInfo(VkDescriptorBufferInfo.calloc(1,s).buffer(vertices.handle()).range(vertices.size()));writes.get(2).pBufferInfo(VkDescriptorBufferInfo.calloc(1,s).buffer(parameters.handle()).range(parameters.size()));
        writes.get(3).pImageInfo(VkDescriptorImageInfo.calloc(1,s).imageView(output.view).imageLayout(VK_IMAGE_LAYOUT_GENERAL));
        for(var entry:source.samplers.entrySet()) {
            TextureImage image=textures.getOrDefault(entry.getKey(),textures.get("Atlas"));
            writes.get(entry.getValue()).pImageInfo(VkDescriptorImageInfo.calloc(1,s).imageView(image.view).sampler(image.sampler).imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL));
        }
        vkUpdateDescriptorSets(vk.device,writes,null);
    }}
    private long pipeline(String code,String name) throws Exception {
        long compiler=shaderc_compiler_initialize(),options=shaderc_compile_options_initialize();
        shaderc_compile_options_set_target_env(options,shaderc_target_env_vulkan,shaderc_env_version_vulkan_1_2);shaderc_compile_options_set_optimization_level(options,shaderc_optimization_level_performance);
        long result=shaderc_compile_into_spv(compiler,code,shaderc_compute_shader,name,"main",options);
        try(MemoryStack s=MemoryStack.stackPush()) {
            if(shaderc_result_get_compilation_status(result)!=shaderc_compilation_status_success)throw new IllegalStateException(shaderc_result_get_error_message(result));
            var out=s.mallocLong(1);check(vkCreateShaderModule(vk.device,VkShaderModuleCreateInfo.calloc(s).sType$Default().pCode(shaderc_result_get_bytes(result)),null,out));long module=out.get(0);
            try {var stage=VkPipelineShaderStageCreateInfo.calloc(s).sType$Default().stage(VK_SHADER_STAGE_COMPUTE_BIT).module(module).pName(s.UTF8("main"));
                check(vkCreateComputePipelines(vk.device,VK_NULL_HANDLE,VkComputePipelineCreateInfo.calloc(1,s).sType$Default().stage(stage).layout(layout),null,out));return out.get(0);
            }finally{vkDestroyShaderModule(vk.device,module,null);}
        }finally{shaderc_result_release(result);shaderc_compile_options_release(options);shaderc_compiler_release(compiler);}
    }
    @Override public int render(Map<String,float[]> values) {
        if(closed)throw new IllegalStateException("Closed RTX backend");
        // Single in-flight frozen frame: host uniform writes and command-buffer reset are fenced.
        check(vkQueueWaitIdle(vk.queue));
        for(var entry:source.uniforms.entrySet()) {float[] value=values.get(entry.getKey());for(int i=0;i<4;i++)parameters.mapped().putFloat(entry.getValue()*16+i*4,value==null?0:value[i]);}
        try(MemoryStack s=MemoryStack.stackPush()) {
            glSignalSemaphoreEXT(glToVk,s.mallocInt(0),s.ints(sharedTexture),s.ints(GL_LAYOUT_GENERAL_EXT));GL11.glFlush();
            vk.begin();barrier(output.image,VK_IMAGE_LAYOUT_GENERAL,VK_IMAGE_LAYOUT_GENERAL,VK_QUEUE_FAMILY_EXTERNAL,vk.queueFamily,0,VK_ACCESS_SHADER_WRITE_BIT);
            vkCmdBindDescriptorSets(vk.command,VK_PIPELINE_BIND_POINT_COMPUTE,layout,0,s.longs(set),null);
            vkCmdBindPipeline(vk.command,VK_PIPELINE_BIND_POINT_COMPUTE,initialPipeline);vkCmdDispatch(vk.command,(width*2+7)/8,(height+7)/8,1);
            barrier(output.image,VK_IMAGE_LAYOUT_GENERAL,VK_IMAGE_LAYOUT_GENERAL,vk.queueFamily,vk.queueFamily,VK_ACCESS_SHADER_WRITE_BIT,VK_ACCESS_SHADER_READ_BIT|VK_ACCESS_SHADER_WRITE_BIT);
            vkCmdBindPipeline(vk.command,VK_PIPELINE_BIND_POINT_COMPUTE,materialPipeline);vkCmdDispatch(vk.command,(width*2+7)/8,(height+7)/8,1);
            barrier(output.image,VK_IMAGE_LAYOUT_GENERAL,VK_IMAGE_LAYOUT_GENERAL,vk.queueFamily,VK_QUEUE_FAMILY_EXTERNAL,VK_ACCESS_SHADER_WRITE_BIT,0);
            vkCmdWriteTimestamp(vk.command,VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,vk.queryPool,1);check(vkEndCommandBuffer(vk.command));
            var submit=VkSubmitInfo.calloc(s).sType$Default().pCommandBuffers(s.pointers(vk.command.address())).pWaitSemaphores(s.longs(toVk)).pWaitDstStageMask(s.ints(VK_PIPELINE_STAGE_ALL_COMMANDS_BIT)).pSignalSemaphores(s.longs(toGl));check(vkQueueSubmit(vk.queue,submit,VK_NULL_HANDLE));
            glWaitSemaphoreEXT(glToGl,s.mallocInt(0),s.ints(sharedTexture),s.ints(GL_LAYOUT_GENERAL_EXT));
        }
        return sharedTexture;
    }
    @Override public String description(){return "RTX frozen full image | two samples + selective materials | "+width+"x"+height;}
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
        if(glToVk!=0)glDeleteSemaphoresEXT(glToVk);if(glToGl!=0)glDeleteSemaphoresEXT(glToGl);
        if(toVk!=0)vkDestroySemaphore(vk.device,toVk,null);if(toGl!=0)vkDestroySemaphore(vk.device,toGl,null);
        vkDestroyPipeline(vk.device,initialPipeline,null);vkDestroyPipeline(vk.device,materialPipeline,null);vkDestroyDescriptorPool(vk.device,descriptorPool,null);vkDestroyPipelineLayout(vk.device,layout,null);vkDestroyDescriptorSetLayout(vk.device,descriptorLayout,null);
        for(int i=cleanup.size()-1;i>=0;i--)cleanup.get(i).run();vk.close();
    }
}
