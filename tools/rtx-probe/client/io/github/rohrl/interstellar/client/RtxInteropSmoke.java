package io.github.rohrl.interstellar.client;

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
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.*;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.*;
import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.opengl.EXTSemaphore.glGenSemaphoresEXT;
import static org.lwjgl.opengl.EXTSemaphore.glDeleteSemaphoresEXT;
import static org.lwjgl.opengl.EXTSemaphore.glWaitSemaphoreEXT;
import static org.lwjgl.opengl.EXTSemaphore.glSignalSemaphoreEXT;
import static org.lwjgl.opengl.EXTSemaphoreWin32.glImportSemaphoreWin32HandleEXT;

/** Windows-only opt-in Vulkan/GL shared-image test, executed in Minecraft's render context.
 * Clears/blits, not a lensing backend. No CPU readback in the timed batches. */
public final class RtxInteropSmoke implements AutoCloseable {
    private VkInstance instance;private VkPhysicalDevice physical;private VkDevice device;private VkQueue queue;
    private VkPhysicalDeviceMemoryProperties memory;private int family;
    private long pool,image,allocation,toGl,toVk;
    private int glMemory,texture,copyTexture,readFbo,drawFbo,glToGl,glToVk;
    private VkCommandBuffer first;private final VkCommandBuffer[] commands=new VkCommandBuffer[32];
    private final int width,height;
    private boolean started;
    private final int oldTexture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),oldRead=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),oldDraw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    private final boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
    private RtxInteropSmoke(int width,int height) throws Exception {
        this.width=width;this.height=height;
        try {init();} catch(Exception failure){close();throw failure;}
    }
    private static void check(int result){if(result!=VK_SUCCESS)throw new IllegalStateException("Vulkan interop: "+result);}
    private static void glCheck(){int e=GL11.glGetError();if(e!=GL11.GL_NO_ERROR)throw new IllegalStateException("OpenGL interop error 0x"+Integer.toHexString(e));}
    private static void closeHandle(long handle){if(JNI.callPI(handle,Kernel32.getLibrary().getFunctionAddress("CloseHandle"))==0)throw new IllegalStateException("CloseHandle failed");}
    private void init() throws Exception {
        var caps=GL.getCapabilities();
        if(!caps.GL_EXT_memory_object_win32||!caps.GL_EXT_semaphore_win32)throw new IllegalStateException("External Win32 memory/semaphore extensions unavailable");
        glCheck();
        try(MemoryStack s=MemoryStack.stackPush()) {
            ByteBuffer glUuid=s.malloc(16);glGetUnsignedBytei_vEXT(GL_DEVICE_UUID_EXT,0,glUuid);
            var app=VkApplicationInfo.calloc(s).sType$Default().apiVersion(VK_API_VERSION_1_1).pApplicationName(s.UTF8("Interstellar interop smoke"));
            var create=VkInstanceCreateInfo.calloc(s).sType$Default().pApplicationInfo(app);PointerBuffer pointer=s.mallocPointer(1);check(vkCreateInstance(create,null,pointer));instance=new VkInstance(pointer.get(0),create);
            var count=s.ints(0);check(vkEnumeratePhysicalDevices(instance,count,null));var devices=s.mallocPointer(count.get(0));check(vkEnumeratePhysicalDevices(instance,count,devices));
            for(int i=0;i<count.get(0);i++) {
                var candidate=new VkPhysicalDevice(devices.get(i),instance);var id=VkPhysicalDeviceIDProperties.calloc(s).sType$Default();
                var props=VkPhysicalDeviceProperties2.calloc(s).sType$Default().pNext(id.address());vkGetPhysicalDeviceProperties2(candidate,props);
                if(id.deviceUUID().equals(glUuid)){physical=candidate;break;}
            }
            if(physical==null)throw new IllegalStateException("Cannot match Vulkan device to Minecraft's OpenGL UUID");
            vkGetPhysicalDeviceQueueFamilyProperties(physical,count,null);var families=VkQueueFamilyProperties.calloc(count.get(0),s);vkGetPhysicalDeviceQueueFamilyProperties(physical,count,families);
            family=-1;for(int i=0;i<count.get(0);i++)if((families.get(i).queueFlags()&VK_QUEUE_GRAPHICS_BIT)!=0){family=i;break;}
            if(family<0)throw new IllegalStateException("No graphics queue");
            var qi=VkDeviceQueueCreateInfo.calloc(1,s).sType$Default().queueFamilyIndex(family).pQueuePriorities(s.floats(1));
            var info=VkDeviceCreateInfo.calloc(s).sType$Default().pQueueCreateInfos(qi).ppEnabledExtensionNames(s.pointers(s.UTF8("VK_KHR_external_memory_win32"),s.UTF8("VK_KHR_external_semaphore_win32")));
            check(vkCreateDevice(physical,info,null,pointer));device=new VkDevice(pointer.get(0),physical,info);vkGetDeviceQueue(device,family,0,pointer);queue=new VkQueue(pointer.get(0),device);
            memory=VkPhysicalDeviceMemoryProperties.calloc();vkGetPhysicalDeviceMemoryProperties(physical,memory);
            var out=s.mallocLong(1);check(vkCreateCommandPool(device,VkCommandPoolCreateInfo.calloc(s).sType$Default().queueFamilyIndex(family),null,out));pool=out.get(0);
            var external=VkExternalMemoryImageCreateInfo.calloc(s).sType$Default().handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);
            var imageInfo=VkImageCreateInfo.calloc(s).sType$Default().pNext(external.address()).imageType(VK_IMAGE_TYPE_2D).format(VK_FORMAT_R8G8B8A8_UNORM).mipLevels(1).arrayLayers(1).samples(VK_SAMPLE_COUNT_1_BIT).tiling(VK_IMAGE_TILING_OPTIMAL).usage(VK_IMAGE_USAGE_TRANSFER_SRC_BIT|VK_IMAGE_USAGE_TRANSFER_DST_BIT|VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT|VK_IMAGE_USAGE_SAMPLED_BIT).sharingMode(VK_SHARING_MODE_EXCLUSIVE).initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.extent().set(width,height,1);
            var externalFormat=VkPhysicalDeviceExternalImageFormatInfo.calloc(s).sType$Default().handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);
            var externalProperties=VkExternalImageFormatProperties.calloc(s).sType$Default();
            check(vkGetPhysicalDeviceImageFormatProperties2(physical,VkPhysicalDeviceImageFormatInfo2.calloc(s).sType$Default().pNext(externalFormat.address()).format(imageInfo.format()).type(imageInfo.imageType()).tiling(imageInfo.tiling()).usage(imageInfo.usage()),VkImageFormatProperties2.calloc(s).sType$Default().pNext(externalProperties.address())));
            if((externalProperties.externalMemoryProperties().externalMemoryFeatures()&VK_EXTERNAL_MEMORY_FEATURE_EXPORTABLE_BIT)==0)throw new IllegalStateException("Image memory is not exportable");
            check(vkCreateImage(device,imageInfo,null,out));image=out.get(0);
            var requirements=VkMemoryRequirements.calloc(s);vkGetImageMemoryRequirements(device,image,requirements);
            var export=VkExportMemoryAllocateInfo.calloc(s).sType$Default().handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);
            var dedicated=VkMemoryDedicatedAllocateInfo.calloc(s).sType$Default().pNext(export.address()).image(image);
            check(vkAllocateMemory(device,VkMemoryAllocateInfo.calloc(s).sType$Default().pNext(dedicated.address()).allocationSize(requirements.size()).memoryTypeIndex(memoryType(requirements.memoryTypeBits(),VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)),null,out));allocation=out.get(0);check(vkBindImageMemory(device,image,allocation,0));
            check(vkGetMemoryWin32HandleKHR(device,VkMemoryGetWin32HandleInfoKHR.calloc(s).sType$Default().memory(allocation).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT),pointer));
            glMemory=glCreateMemoryObjectsEXT();glMemoryObjectParameteriEXT(glMemory,GL_DEDICATED_MEMORY_OBJECT_EXT,GL11.GL_TRUE);
            try {glImportMemoryWin32HandleEXT(glMemory,requirements.size(),EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT,pointer.get(0));}finally {closeHandle(pointer.get(0));}
            texture=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL_TEXTURE_TILING_EXT,GL_OPTIMAL_TILING_EXT);glTexStorageMem2DEXT(GL11.GL_TEXTURE_2D,1,GL11.GL_RGBA8,width,height,glMemory,0);
            copyTexture=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,copyTexture);GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,width,height,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,(ByteBuffer)null);
            readFbo=GL30.glGenFramebuffers();drawFbo=GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,readFbo);GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture,0);GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,drawFbo);GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,copyTexture,0);GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            if(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE||GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Interop FBO incomplete");
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            long[] a=semaphore(),b=semaphore();toGl=a[0];glToGl=(int)a[1];toVk=b[0];glToVk=(int)b[1];
            first=record(true,0);for(int i=0;i<commands.length;i++)commands[i]=record(false,i);
            glCheck();
        }
    }
    private int memoryType(int bits,int flags){for(int i=0;i<memory.memoryTypeCount();i++)if((bits&(1<<i))!=0&&(memory.memoryTypes(i).propertyFlags()&flags)==flags)return i;throw new IllegalStateException("No interop memory type");}
    private long[] semaphore(){try(MemoryStack s=MemoryStack.stackPush()){
        var out=s.mallocLong(1);var export=VkExportSemaphoreCreateInfo.calloc(s).sType$Default().handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT);
        check(vkCreateSemaphore(device,VkSemaphoreCreateInfo.calloc(s).sType$Default().pNext(export.address()),null,out));long vk=out.get(0);int gl=glGenSemaphoresEXT();
        var handle=s.mallocPointer(1);check(vkGetSemaphoreWin32HandleKHR(device,VkSemaphoreGetWin32HandleInfoKHR.calloc(s).sType$Default().semaphore(vk).handleType(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT),handle));
        try {glImportSemaphoreWin32HandleEXT(gl,EXTSemaphoreWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT,handle.get(0));}finally {closeHandle(handle.get(0));}
        return new long[]{vk,gl};
    }}
    private VkCommandBuffer command(){try(MemoryStack s=MemoryStack.stackPush()) {var pointer=s.mallocPointer(1);check(vkAllocateCommandBuffers(device,VkCommandBufferAllocateInfo.calloc(s).sType$Default().commandPool(pool).level(VK_COMMAND_BUFFER_LEVEL_PRIMARY).commandBufferCount(1),pointer));return new VkCommandBuffer(pointer.get(0),device);}}
    private VkCommandBuffer record(boolean initial,int colour){
        VkCommandBuffer cb=command();try(MemoryStack s=MemoryStack.stackPush()) {
            check(vkBeginCommandBuffer(cb,VkCommandBufferBeginInfo.calloc(s).sType$Default()));
            barrier(cb,initial?VK_IMAGE_LAYOUT_UNDEFINED:VK_IMAGE_LAYOUT_GENERAL,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,initial?family:VK_QUEUE_FAMILY_EXTERNAL,family,0,VK_ACCESS_TRANSFER_WRITE_BIT);
            var range=VkImageSubresourceRange.calloc(1,s).aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(1).layerCount(1);
            var clear=VkClearColorValue.calloc(s).float32(0,(colour&1)).float32(1,(colour>>1)&1).float32(2,(colour>>2)&1).float32(3,1);
            vkCmdClearColorImage(cb,image,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,clear,range);
            barrier(cb,VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,VK_IMAGE_LAYOUT_GENERAL,family,VK_QUEUE_FAMILY_EXTERNAL,VK_ACCESS_TRANSFER_WRITE_BIT,0);
            check(vkEndCommandBuffer(cb));return cb;
        }
    }
    private void barrier(VkCommandBuffer cb,int before,int after,int sourceFamily,int destinationFamily,int sourceAccess,int destinationAccess){try(MemoryStack s=MemoryStack.stackPush()){
        var b=VkImageMemoryBarrier.calloc(1,s).sType$Default().oldLayout(before).newLayout(after).srcQueueFamilyIndex(sourceFamily).dstQueueFamilyIndex(destinationFamily).srcAccessMask(sourceAccess).dstAccessMask(destinationAccess).image(image);
        b.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(1).layerCount(1);
        vkCmdPipelineBarrier(cb,VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,0,null,null,b);
    }}
    private void frame(int colour){try(MemoryStack s=MemoryStack.stackPush()){
        var submit=VkSubmitInfo.calloc(s).sType$Default().pCommandBuffers(s.pointers((started?commands[colour]:first).address())).pSignalSemaphores(s.longs(toGl));
        if(started)submit.pWaitSemaphores(s.longs(toVk)).pWaitDstStageMask(s.ints(VK_PIPELINE_STAGE_ALL_COMMANDS_BIT));
        check(vkQueueSubmit(queue,submit,VK_NULL_HANDLE));started=true;
        // LWJGL's checked overload requires a buffer even when the barrier count is zero.
        glWaitSemaphoreEXT(glToGl,s.mallocInt(0),s.ints(texture),s.ints(EXTSemaphore.GL_LAYOUT_GENERAL_EXT));
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,readFbo);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,drawFbo);
        GL30.glBlitFramebuffer(0,0,width,height,0,0,width,height,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);
        glSignalSemaphoreEXT(glToVk,s.mallocInt(0),s.ints(texture),s.ints(EXTSemaphore.GL_LAYOUT_GENERAL_EXT));GL11.glFlush();
    }}
    private void verify(int colour){try(MemoryStack s=MemoryStack.stackPush()){
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,drawFbo);var bytes=s.malloc(4);
        for(int[] xy:new int[][]{{0,0},{width-1,0},{0,height-1},{width-1,height-1},{width/2,height/2}}){GL11.glReadPixels(xy[0],xy[1],1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,bytes);
            for(int a=0;a<4;a++){int expected=a==3?255:((colour>>a)&1)*255;if(Byte.toUnsignedInt(bytes.get(a))!=expected)throw new IllegalStateException("Interop stale/wrong pixel at "+Arrays.toString(xy));}}
        glCheck();
    }}
    private String measure(){
        for(int i=0;i<8;i++){frame(i);GL11.glFinish();verify(i);}
        double[] wall=new double[5],gpu=new double[5];int q0=GL15.glGenQueries(),q1=GL15.glGenQueries();
        try {for(int batch=0;batch<5;batch++){
            GL11.glFinish();long start=System.nanoTime();GL33.glQueryCounter(q0,GL33.GL_TIMESTAMP);
            for(int i=1;i<32;i++)frame(i);
            GL33.glQueryCounter(q1,GL33.GL_TIMESTAMP);GL11.glFinish();wall[batch]=(System.nanoTime()-start)/1e6/31;
            gpu[batch]=(GL33.glGetQueryObjectui64(q1,GL15.GL_QUERY_RESULT)-GL33.glGetQueryObjectui64(q0,GL15.GL_QUERY_RESULT))/1e6/31;verify(31);
        }}finally {GL15.glDeleteQueries(q0);GL15.glDeleteQueries(q1);}
        return "size="+width+"x"+height+" deviceUUIDMatch=true importedSharedMemory=true correctness=65_pixels_exact frames=163 wallMsPerFrame="+Arrays.toString(wall)+" glTimelineMsPerFrame="+Arrays.toString(gpu)+"\n";
    }
    public static void run(Path directory) throws Exception {
        StringBuilder result=new StringBuilder("Minecraft OpenGL context + Vulkan shared RGBA8 image; Win32 opaque handles; dedicated allocation; two external semaphores; Vulkan clear -> GL blit -> Vulkan ownership return.\n8 verified warmups; 5 batches of31 frames. No readback or per-frame CPU wait inside timed batches. Not a lensing backend or full frame-overhead estimate.\n");
        for(int[] size:new int[][]{{854,480},{2560,1440}})try(var test=new RtxInteropSmoke(size[0],size[1])){result.append(test.measure());}
        Files.writeString(directory.resolve("interop.txt"),result);
        io.github.rohrl.interstellar.Interstellar.LOGGER.info("RTX interop smoke completed: {}",result);
    }
    @Override public void close(){
        GL11.glFinish();if(device!=null)vkDeviceWaitIdle(device);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,oldRead);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,oldDraw);GL11.glBindTexture(GL11.GL_TEXTURE_2D,oldTexture);if(scissor)GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL30.glDeleteFramebuffers(readFbo);GL30.glDeleteFramebuffers(drawFbo);GL11.glDeleteTextures(texture);GL11.glDeleteTextures(copyTexture);
        if(glToGl!=0)glDeleteSemaphoresEXT(glToGl);if(glToVk!=0)glDeleteSemaphoresEXT(glToVk);if(glMemory!=0)glDeleteMemoryObjectsEXT(glMemory);
        if(device!=null){if(toGl!=0)vkDestroySemaphore(device,toGl,null);if(toVk!=0)vkDestroySemaphore(device,toVk,null);if(image!=0)vkDestroyImage(device,image,null);if(allocation!=0)vkFreeMemory(device,allocation,null);if(pool!=0)vkDestroyCommandPool(device,pool,null);vkDestroyDevice(device,null);}
        if(memory!=null)memory.free();if(instance!=null)vkDestroyInstance(instance,null);
    }
}
