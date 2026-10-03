import java.nio.file.*;
import java.util.regex.*;
import io.github.rohrl.interstellar.client.WorldRenderBackend.Optics;
import static org.lwjgl.util.shaderc.Shaderc.*;

/** Fast headless compilation of every production RTX optics/material variant. */
public final class ShaderCheck {
    private static String expand(String name) throws Exception {
        String text=Files.readString(Path.of("src/main/resources/assets/interstellar/shaders/include",name));
        var pattern=Pattern.compile("#moj_import <interstellar:([^>]+)>").matcher(text);
        var out=new StringBuilder();
        while(pattern.find())pattern.appendReplacement(out,Matcher.quoteReplacement(expand(pattern.group(1))));
        pattern.appendTail(out);return out.toString();
    }
    public static void main(String[] args) throws Exception {
        long compiler=shaderc_compiler_initialize(),options=shaderc_compile_options_initialize();
        try {
            shaderc_compile_options_set_target_env(options,shaderc_target_env_vulkan,shaderc_env_version_vulkan_1_2);
            shaderc_compile_options_set_optimization_level(options,shaderc_optimization_level_performance);
            String source=expand("terrain_shared.glsl");int count=0;
            for(boolean live:new boolean[]{false,true})for(var optics:Optics.values()) {
                var shader=new FullImageShader(source,live,optics);
                for(String code:new String[]{shader.probe,shader.material}) {
                    String name=optics+"-"+(live?"live":"frozen")+"-"+count;
                    long result=shaderc_compile_into_spv(compiler,code,shaderc_compute_shader,name,"main",options);
                    try {
                        if(shaderc_result_get_compilation_status(result)!=shaderc_compilation_status_success)
                            throw new IllegalStateException(shaderc_result_get_error_message(result));
                        count++;
                    } finally {shaderc_result_release(result);}
                }
            }
            System.out.println("RTX shader check passed: "+count+" compute programs");
        } finally {shaderc_compile_options_release(options);shaderc_compiler_release(compiler);}
    }
}
