import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.util.shaderc.Shaderc.*;

/** Large generated shader sources must not depend on the launcher's LWJGL stack size. */
final class RtxShaderCompiler {
    private RtxShaderCompiler() {}
    static long compile(long compiler,String source,int kind,String name,String entry,long options) {
        var bytes=MemoryUtil.memUTF8(source,false);
        try (var stack=MemoryStack.stackPush()) {
            return shaderc_compile_into_spv(compiler,bytes,kind,stack.UTF8(name),stack.UTF8(entry),options);
        } finally {MemoryUtil.memFree(bytes);}
    }
}
