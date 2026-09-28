import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** JDK-only comparison of a static terrain crop; actors may differ across runs.
 * Usage: java tools/LensBoundaryEvidence.java before-pair after-pair output-dir
 * Each pair contains candidate.png, reference.png and metadata.json.
 */
public class LensBoundaryEvidence {
    public static void main(String[] args) throws Exception {
        Path beforeDir=Path.of(args[0]),afterDir=Path.of(args[1]),out=Path.of(args[2]);
        String beforeMeta=Files.readString(beforeDir.resolve("metadata.json"));
        String afterMeta=Files.readString(afterDir.resolve("metadata.json"));
        for(String key:new String[]{"camera","yaw","pitch","projection","qualitySettings","width","height"}) {
            var pattern=Pattern.compile("\""+key+"\"\\s*:\\s*(\\[[^]]*]|\"[^\"]*\"|[^,}]+)");
            var a=pattern.matcher(beforeMeta);var b=pattern.matcher(afterMeta);
            if(!a.find() || !b.find() || !a.group(1).equals(b.group(1)))
                throw new IllegalArgumentException("Mismatched camera/quality: "+key);
        }
        var before=ImageIO.read(beforeDir.resolve("candidate.png").toFile());
        var after=ImageIO.read(afterDir.resolve("candidate.png").toFile());
        var reference=ImageIO.read(afterDir.resolve("reference.png").toFile());
        // Fixed crop of distant terrain/coloured wall: no moving actors or sky.
        int x=355,y=460,w=250,h=145;
        String csv="image,region,mae255,rmse255,pixelsAbove16,pixels\n"
                +metrics("before","terrain-crop",before,reference,x,y,w,h)
                +metrics("after","terrain-crop",after,reference,x,y,w,h)
                +metrics("after","full-frame",after,reference,0,0,after.getWidth(),after.getHeight());
        Files.createDirectories(out);Files.writeString(out.resolve("metrics.csv"),csv);
        var crop=new BufferedImage(2*w,h+24,BufferedImage.TYPE_INT_RGB);var g=crop.createGraphics();
        g.setColor(Color.WHITE);g.drawString("Before",8,16);g.drawString("After",w+8,16);
        g.drawImage(before.getSubimage(x,y,w,h),0,24,null);
        g.drawImage(after.getSubimage(x,y,w,h),w,24,null);g.dispose();
        ImageIO.write(crop,"png",out.resolve("before-after.png").toFile());
        System.out.print(csv);
    }
    private static String metrics(String label,String region,BufferedImage image,BufferedImage reference,int x,int y,int w,int h) {
        if(image.getWidth()!=reference.getWidth() || image.getHeight()!=reference.getHeight())
            throw new IllegalArgumentException("Image dimensions differ");
        long sum=0,square=0,over=0;
        for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++) {
            int a=image.getRGB(xx,yy),b=reference.getRGB(xx,yy),max=0;
            for(int k=0;k<24;k+=8){int d=Math.abs((a>>k&255)-(b>>k&255));sum+=d;square+=(long)d*d;max=Math.max(max,d);}
            if(max>16)over++;
        }
        return String.format(Locale.ROOT,"%s,%s,%.8f,%.8f,%d,%d%n",label,region,sum/(3.*w*h),Math.sqrt(square/(3.*w*h)),over,w*h);
    }
}
