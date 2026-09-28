import java.util.*;
import java.util.regex.*;
import io.github.rohrl.interstellar.client.WorldRenderBackend.Optics;

/** Builds the experimental compute programs from the SAME optical/material source as OpenGL.
 * Marker checks deliberately fail if that source changes incompatibly. No hand-copied orbit solver. */
final class FullImageShader {
    final LinkedHashMap<String,Integer> uniforms=new LinkedHashMap<>(),samplers=new LinkedHashMap<>();
    final String probe,material;
    final int movingBinding;
    FullImageShader(String original) {this(original,false);}
    FullImageShader(String original,boolean live) {this(original,live,Optics.EXTERIOR);}
    FullImageShader(String original,boolean live,Optics optics) {
        String source=original.replaceAll("(?m)^#moj_import[^\\n]*","");
        int start=source.indexOf("#ifdef INTERSTELLAR_QUAD_MESH\nvec4 quadPart");
        if(start<0) {source=source.replace("\r\n","\n");start=source.indexOf("#ifdef INTERSTELLAR_QUAD_MESH\nvec4 quadPart");}
        int end=source.indexOf("vec3 emptyLow",start);
        if(start<0||end<0)throw new IllegalStateException("Triangle-access source markers changed");
        source=source.substring(0,start)+"vec4 trianglePart(int tree,int base,int part) {vec4 v=rtxVertices[base+part];if(part==0)v.w=mod(v.w,256.0)-128.0;return v;}\n"+source.substring(end);
        if(live)source=source.replace("vec4 trianglePart(int tree,int base,int part) {vec4 v=rtxVertices[base+part];if(part==0)v.w=mod(v.w,256.0)-128.0;return v;}","""
            int rtxHalf;
            vec4 trianglePart(int tree,int base,int part) {
                if(tree!=0)return rtxMoving[base+part];
                int corner=part/3;
                if(rtxHalf!=0)corner=corner==0?2:corner==1?3:0;
                return rtxVertices[base+corner*3+part%3];
            }
            """);
        start=source.indexOf("int meshSegment(");end=source.indexOf("vec3 surface(",start);
        String candidate=between(source,"            COUNT_WORK(4+min(tree,1));","            DETAIL_END(shadeClock,1);\n        }");
        String intersection=between(candidate,"            vec3 s=start-a;","#ifdef INTERSTELLAR_HORIZON\n            if(interiorCamera");
        candidate=candidate.replace(intersection,"            vec2 bary=rayQueryGetIntersectionBarycentricsEXT(query,false);float u=bary.x,v=bary.y;\n            float t=rayQueryGetIntersectionTEXT(query,false);if(t<0 || t>1 || t>=best)continue;\n");
        // The material cloud branch accepts a hit before continuing; the probe cloud branch does not.
        candidate=candidate.replace("found=true;{DETAIL_END(shadeClock,1);continue;}","found=true;rayQueryConfirmIntersectionEXT(query);{DETAIL_END(shadeClock,1);continue;}");
        String search="""
            int meshSegment(vec3 start,vec3 end,out vec3 hit,out vec3 normal) {
                vec3 delta=end-start;float best=1.000001;bool found=false;
                meshAlpha=1.0;
            #ifdef INTERSTELLAR_MATERIALS
                meshCloud=false;
            #endif
                float cloudAt=2.0;vec4 nearestCloud=vec4(0);
                uint mask=7u;
            #ifdef INTERSTELLAR_MATERIALS
                if(cloudSeen)mask=3u;
            #else
                if(cloudLayer.a>0.0)mask=3u;
            #endif
                rayQueryEXT query;
                rayQueryInitializeEXT(query,rtxScene,0u,mask,start,0.0,delta,1.0);
                while(rayQueryProceedEXT(query)) {
                    int base=int(rayQueryGetIntersectionInstanceCustomIndexEXT(query,false)+rayQueryGetIntersectionPrimitiveIndexEXT(query,false))*9;
                    int tree=0;
            """+candidate+"\nrayQueryConfirmIntersectionEXT(query);\n}\n"+"""
            #ifndef INTERSTELLAR_MATERIALS
                if(cloudAt<best && cloudLayer.a==0.0)cloudLayer=nearestCloud;
            #endif
                return found?3:-1;
            }
            """;
        source=source.substring(0,start)+search+source.substring(end);
        if(live) {
            String old="int base=int(rayQueryGetIntersectionInstanceCustomIndexEXT(query,false)+rayQueryGetIntersectionPrimitiveIndexEXT(query,false))*9;";
            if(!source.contains(old))throw new IllegalStateException("Live query source marker changed");
            source=source.replaceAll(Pattern.quote(old)+"\\s*int tree=0;",Matcher.quoteReplacement("uint instance=rayQueryGetIntersectionInstanceCustomIndexEXT(query,false);int primitive=int(rayQueryGetIntersectionPrimitiveIndexEXT(query,false));int tree=int(instance>>23);rtxHalf=primitive%2;int base=tree==0?(int(instance)+primitive/2)*12:(int(instance&0x7fffffu)+primitive)*9;"));
        }
        var numeric=Pattern.compile("uniform\\s+(float|vec[234])\\s+([^;]+);").matcher(source);var out=new StringBuilder();
        while(numeric.find()) {
            StringBuilder replacement=new StringBuilder();
            for(String name:numeric.group(2).split(",")) {
                name=name.trim();
                if(name.equals("SampleOffset")){replacement.append("float SampleOffset;\n");continue;}
                int index=uniforms.computeIfAbsent(name,k->uniforms.size());
                String swizzle=switch(numeric.group(1)){case "float"->"x";case "vec2"->"xy";case "vec3"->"xyz";default->"xyzw";};
                replacement.append("\n#define ").append(name).append(" rtxParameters[").append(index).append("].").append(swizzle).append('\n');
            }
            numeric.appendReplacement(out,Matcher.quoteReplacement(replacement.toString()));
        }
        numeric.appendTail(out);source=out.toString();
        var textures=Pattern.compile("uniform sampler2D ([^;]+);").matcher(source);out=new StringBuilder();
        while(textures.find()) {
            StringBuilder replacement=new StringBuilder();
            for(String name:textures.group(1).split(",")) {name=name.trim();int binding=samplers.computeIfAbsent(name,k->4+samplers.size());replacement.append("layout(set=0,binding=").append(binding).append(") uniform sampler2D ").append(name).append(";\n");}
            textures.appendReplacement(out,Matcher.quoteReplacement(replacement.toString()));
        }
        textures.appendTail(out);source=out.toString();
        movingBinding=4+samplers.size();
        source=source.replace("in vec2 screenUv;","vec2 screenUv;").replace("out vec4 fragColor;","vec4 fragColor;");
        source=source.replace("if(texelFetch(PendingRays,ivec2(gl_FragCoord.xy),0).a>.5)discard;","");
        source=source.replace("void main() {","void opticalMain() {");
        String header="""
            #version 460
            #extension GL_EXT_ray_query : require
            layout(local_size_x=8,local_size_y=8) in;
            layout(set=0,binding=0) uniform accelerationStructureEXT rtxScene;
            layout(std430,set=0,binding=1) readonly buffer RtxVertices {vec4 rtxVertices[];};
            layout(std140,set=0,binding=2) uniform RtxParameters {vec4 rtxParameters[128];};
            layout(rgba32f,set=0,binding=3) uniform image2D rtxOutput;
            vec4 rtxFragCoord;
            #define gl_FragCoord rtxFragCoord
            #define INTERSTELLAR_SPLIT_MOVING
            #define INTERSTELLAR_NATIVE_MESH
            #define INTERSTELLAR_LIVE_DEFAULTS
            #define INTERSTELLAR_VARIABLE_CHORD
            #define INTERSTELLAR_SPLIT_AA
            """;
        String main="""
            void main() {
                ivec2 pixel=ivec2(gl_GlobalInvocationID.xy);
                if(pixel.x>=int(Viewport.x)*2 || pixel.y>=int(Viewport.y))return;
            #ifdef INTERSTELLAR_MATERIAL_MASK
                if(imageLoad(rtxOutput,pixel).a>.5)return;
            #endif
                rtxFragCoord=vec4(vec2(pixel)+.5,0,1);
                SampleOffset=pixel.x<int(Viewport.x)?-.25:.25;
                screenUv=vec2((float(pixel.x%int(Viewport.x))+.5)/Viewport.x,1.0-(float(pixel.y)+.5)/Viewport.y);
                opticalMain();imageStore(rtxOutput,pixel,fragColor);
            }
            """;
        if(live)header+="layout(std430,set=0,binding="+movingBinding+") readonly buffer RtxMoving {vec4 rtxMoving[];};\n";
        header+=switch(optics) {
            case EXTERIOR -> "";
            case EXTENDED -> "#define INTERSTELLAR_EXTENDED_SOURCE\n";
            case HORIZON -> "#define INTERSTELLAR_HORIZON\n";
        };
        probe=header+"#define INTERSTELLAR_MATERIAL_PROBE\n"+source+main;
        material=header+"#define INTERSTELLAR_MATERIALS\n#define INTERSTELLAR_MATERIAL_MASK\n"+source+main;
        if(uniforms.size()>128)throw new IllegalStateException("Uniform block capacity exceeded");
    }
    private static String between(String source,String start,String end) {
        int a=source.indexOf(start),b=source.indexOf(end,a);
        if(a<0||b<0)throw new IllegalStateException("Shared shading source markers changed: "+start);
        return source.substring(a,b);
    }
}
