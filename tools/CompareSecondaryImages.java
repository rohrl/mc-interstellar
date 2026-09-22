import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Locale;
import java.util.regex.Pattern;

/** Fixed-pose near-critical annulus metrics. This region is a proxy, not a claim that every ray makes a secondary image. */
public class CompareSecondaryImages {
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("Usage: java tools/CompareSecondaryImages.java PAIR_DIRECTORY SOURCE_X SOURCE_Y SOURCE_Z RADIUS");
        Path dir=Path.of(args[0]);String metadata=Files.readString(dir.resolve("metadata.json"));
        var cameraMatch=Pattern.compile("\\\"camera\\\"\\s*:\\s*\\[([^]]+)]").matcher(metadata);
        if(!cameraMatch.find())throw new IllegalArgumentException("Camera metadata missing");
        String[] xyz=cameraMatch.group(1).split(",");double[] radial=new double[3];double r2=0;
        for(int i=0;i<3;i++){radial[i]=Double.parseDouble(xyz[i].trim())-Double.parseDouble(args[i+1]);r2+=radial[i]*radial[i];}
        double r=Math.sqrt(r2),u=Double.parseDouble(args[4])/r;
        if(!(u>0 && u<1/1.05))throw new IllegalArgumentException("Exterior static observer required");
        for(int i=0;i<3;i++)radial[i]/=r;
        double yaw=Math.toRadians(number(metadata,"yaw")),pitch=Math.toRadians(number(metadata,"pitch"));
        double cy=Math.cos(yaw),sy=Math.sin(yaw),cp=Math.cos(pitch),sp=Math.sin(pitch);
        double[] forward={-sy*cp,-sp,cy*cp},right={-cy,0,-sy},up={-sy*sp,cp,cy*sp};
        var slopeMatch=Pattern.compile("\\\"candidateRaySlopes\\\"\\s*:\\s*\\{([^}]+)}").matcher(metadata);
        if(!slopeMatch.find())throw new IllegalArgumentException("Projection metadata missing");
        String slopes=slopeMatch.group(1);double sx=number(slopes,"x"),sz=number(slopes,"y"),ox=number(slopes,"offsetX"),oy=number(slopes,"offsetY");
        var a=ImageIO.read(dir.resolve("reference.png").toFile());var b=ImageIO.read(dir.resolve("candidate.png").toFile());
        if(a==null || b==null || a.getWidth()!=b.getWidth() || a.getHeight()!=b.getHeight())throw new IllegalArgumentException("Images differ in dimensions");
        int w=a.getWidth(),h=a.getHeight(),count=0,changed=0,edgeCount=0,edgeChanged=0,x0=w,y0=h,x1=-1,y1=-1;long error=0,edgeError=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            double dx=(2*(x+.5)/w-1)*sx+ox,dy=-(2*(y+.5)/h-1)*sz+oy,mu=0;
            for(int axis=0;axis<3;axis++)mu+=(forward[axis]+dx*right[axis]+dy*up[axis])*radial[axis];
            mu/=Math.sqrt(1+dx*dx+dy*dy);
            double impact=(1-mu*mu)/(u*u*(1-u));
            if(mu>=0 || impact<6.70 || impact>8)continue;
            int rgbA=a.getRGB(x,y),rgbB=b.getRGB(x,y),max=0,sum=0;
            for(int shift:new int[]{0,8,16}) {int e=Math.abs((rgbA>>shift&255)-(rgbB>>shift&255));sum+=e;max=Math.max(max,e);}
            if(impact>=6.75){error+=sum;count++;if(max>8)changed++;}
            if(impact<=6.85){edgeError+=sum;edgeCount++;if(max>8)edgeChanged++;}
            x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);
        }
        if(count==0)throw new IllegalArgumentException("No near-critical pixels in this view");
        double mae=error/(count*3.0*255),percent=100.0*changed/count;
        String report=String.format(Locale.ROOT,"{\"region\":\"incoming rays with impact squared in [27/4,8]\",\"pixels\":%d,\"rgbMAE\":%.8f,\"pixelsChangedOver8LevelsPercent\":%.5f,\"edgeRegion\":[6.70,6.85],\"edgePixels\":%d,\"edgeRgbMAE\":%.8f,\"edgeChangedOver8LevelsPercent\":%.5f,\"source\":[%s,%s,%s],\"radius\":%s}\n",count,mae,percent,edgeCount,edgeError/(edgeCount*3.0*255),100.0*edgeChanged/edgeCount,args[1],args[2],args[3],args[4]);
        Files.writeString(dir.resolve("secondary-metrics.json"),report);
        var sheet=new BufferedImage((x1-x0+1)*2,y1-y0+1,BufferedImage.TYPE_INT_RGB);var g=sheet.createGraphics();
        try {g.drawImage(a.getSubimage(x0,y0,x1-x0+1,y1-y0+1),0,0,null);g.drawImage(b.getSubimage(x0,y0,x1-x0+1,y1-y0+1),x1-x0+1,0,null);}finally {g.dispose();}
        ImageIO.write(sheet,"PNG",dir.resolve("secondary-contact.png").toFile());
        System.out.print(dir.getFileName()+" "+report);
    }
    private static double number(String text,String key) {
        var match=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*([-+0-9.eE]+)").matcher(text);
        if(!match.find())throw new IllegalArgumentException("Missing "+key);return Double.parseDouble(match.group(1));
    }
}
