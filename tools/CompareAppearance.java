import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** Java 21 source launcher; no downloaded packages. Metrics are diagnostics, not perceptual certification. */
public class CompareAppearance {
    record Stats(double mae,double linearLumaMae,double p95MaxChannel,double changedPercent) {}
    static final double[] LINEAR=new double[256];
    static {for(int i=0;i<256;i++){double s=i/255.;LINEAR[i]=s<=.04045?s/12.92:Math.pow((s+.055)/1.055,2.4);}}
    public static void main(String[] args) throws Exception {
        if(args.length==1 && args[0].equals("--self-test")) {selfTest();return;}
        if(args.length!=1 && args.length!=3)throw new IllegalArgumentException("Usage: java tools/CompareAppearance.java PAIR_DIRECTORY | REFERENCE.png CANDIDATE.png OUTPUT_DIRECTORY | --self-test");
        Path reference,candidate,out;
        if(args.length==1) {
            out=Path.of(args[0]);reference=out.resolve("reference.png");candidate=out.resolve("candidate.png");
            if(!Files.isRegularFile(out.resolve("metadata.json")))throw new IllegalArgumentException("Pair metadata missing");
        } else {reference=Path.of(args[0]);candidate=Path.of(args[1]);out=Path.of(args[2]);}
        var a=ImageIO.read(reference.toFile());var b=ImageIO.read(candidate.toFile());
        if(a==null || b==null)throw new IllegalArgumentException("Unsupported image");
        if(a.getWidth()!=b.getWidth() || a.getHeight()!=b.getHeight())throw new IllegalArgumentException("Dimensions differ; images must not be resized/aligned to hide projection errors");
        int w=a.getWidth(),h=a.getHeight();
        if(w<4 || h<4)throw new IllegalArgumentException("Images too small");
        Files.createDirectories(out);
        var regions=new LinkedHashMap<String,Rectangle>();
        regions.put("full",new Rectangle(0,0,w,h));
        regions.put("top_third",new Rectangle(0,0,w,h/3));
        regions.put("bottom_half",new Rectangle(0,h/2,w,h-h/2));
        regions.put("centre",new Rectangle(w/4,h/4,w/2,h/2));
        StringBuilder json=new StringBuilder("{\n  \"metricSpace\": \"MAE and p95: encoded RGB [0,1]; luminance: linear sRGB [0,1]\",\n  \"automaticAcceptance\": false,\n  \"regions\": {\n");
        boolean first=true;
        for(var region:regions.entrySet()) {
            Stats s=stats(a,b,region.getValue());
            if(!first)json.append(",\n");first=false;
            json.append(String.format(Locale.ROOT,"    \"%s\": {\"rgbMAE\": %.8f, \"linearLumaMAE\": %.8f, \"p95MaxChannel\": %.8f, \"pixelsChangedOver8LevelsPercent\": %.4f}",region.getKey(),s.mae,s.linearLumaMae,s.p95MaxChannel,s.changedPercent));
            System.out.printf(Locale.ROOT,"%s: RGB MAE %.4f | linear luma MAE %.4f | p95 max-channel %.4f | >8/255 %.2f%%%n",region.getKey(),s.mae,s.linearLumaMae,s.p95MaxChannel,s.changedPercent);
        }
        double worst=-1;Rectangle worstRect=null;
        for(int y=0;y<h;y+=32)for(int x=0;x<w;x+=32) {
            var r=new Rectangle(x,y,Math.min(32,w-x),Math.min(32,h-y));double error=stats(a,b,r).mae;
            if(error>worst) {worst=error;worstRect=r;}
        }
        json.append(String.format(Locale.ROOT,"\n  },\n  \"worst32PixelTile\": {\"x\": %d, \"y\": %d, \"width\": %d, \"height\": %d, \"rgbMAE\": %.8f}\n}\n",worstRect.x,worstRect.y,worstRect.width,worstRect.height,worst));
        Files.writeString(out.resolve("metrics.json"),json);
        var heat=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int error=maxError(a.getRGB(x,y),b.getRGB(x,y));
            heat.setRGB(x,y,(Math.min(255,error*4)<<16)|(Math.min(255,Math.max(0,error-64)*2)<<8));
        }
        ImageIO.write(heat,"PNG",out.resolve("difference.png").toFile());
        int panel=Math.min(320,w),height=Math.max(1,(int)Math.round((double)h*panel/w));
        var sheet=new BufferedImage(panel*3,height+24,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();
        try {
            g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));
            var images=List.of(a,b,heat);var labels=List.of("Vanilla reference","Zero-bending candidate","Max RGB error (4x red scale)");
            for(int i=0;i<3;i++){g.drawString(labels.get(i),i*panel+4,16);g.drawImage(images.get(i),i*panel,24,panel,height,null);}
        } finally {g.dispose();}
        ImageIO.write(sheet,"PNG",out.resolve("contact.png").toFile());
        System.out.println("Report: "+out.toAbsolutePath()+" (no acceptance threshold; raw three-path mode does not check scene metadata)");
    }
    static Stats stats(BufferedImage a,BufferedImage b,Rectangle r) {
        long total=0,changed=0;double luma=0;int[] hist=new int[256];int count=r.width*r.height;
        for(int y=r.y;y<r.y+r.height;y++)for(int x=r.x;x<r.x+r.width;x++) {
            int p=a.getRGB(x,y),q=b.getRGB(x,y),max=0;double lp=0,lq=0;
            for(int c=0;c<3;c++) {
                int shift=16-8*c,pc=(p>>shift)&255,qc=(q>>shift)&255,e=Math.abs(pc-qc);
                total+=e;max=Math.max(max,e);double weight=c==0?.2126:c==1?.7152:.0722;
                lp+=weight*LINEAR[pc];lq+=weight*LINEAR[qc];
            }
            hist[max]++;if(max>8)changed++;luma+=Math.abs(lp-lq);
        }
        int cumulative=0,p95=0;for(;p95<255;p95++){cumulative+=hist[p95];if(cumulative>=Math.ceil(count*.95))break;}
        return new Stats(total/(count*3.*255),luma/count,p95/255.,changed*100./count);
    }
    static int maxError(int a,int b) {int max=0;for(int shift=0;shift<=16;shift+=8)max=Math.max(max,Math.abs(((a>>shift)&255)-((b>>shift)&255)));return max;}
    static void selfTest() {
        var black=new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB);var white=new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)white.setRGB(x,y,0xffffff);
        Rectangle all=new Rectangle(0,0,16,16);
        require(stats(black,black,all).equals(new Stats(0,0,0,0)),"identity");
        Stats extreme=stats(black,white,all);require(Math.abs(extreme.mae-1)<1e-12 && Math.abs(extreme.linearLumaMae-1)<1e-12 && extreme.p95MaxChannel==1 && extreme.changedPercent==100,"black/white extremes");
        var patch=new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB);patch.setRGB(0,0,0xffffff);
        Stats tiny=stats(black,patch,all);require(Math.abs(tiny.mae-1./256)<1e-12 && tiny.p95MaxChannel==0,"small local error must not disappear in mean");
        require(stats(black,patch,new Rectangle(0,0,1,1)).mae==1,"region exposes small error");
        require(stats(white,black,all).equals(extreme),"symmetry");
        System.out.println("Comparison self-tests passed: identity, extreme, local error, region, symmetry");
    }
    static void require(boolean condition,String name) {if(!condition)throw new AssertionError(name);}
}
