package io.github.rohrl.interstellar.client;

import io.github.rohrl.interstellar.gravity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/** Render-thread scope shared by native and curved string capture. */
public final class StringGravity {
    public record Context(Vec3d base,double length,GravityField field) {}
    public static Context current;
    private StringGravity() {}
    public static Context context(Vec3d base,double length) {
        var client=MinecraftClient.getInstance();var source=SelectedSource.current();
        if(source==null || !WorldFeatures.gravityEnabled || client.world==null || client.world.getRegistryKey().getValue().toString().equals("interstellar:demo"))return null;
        var field=new GravityField(source.x(),source.y(),source.z(),source.count(),source.enclosingRadius(),source.blackHoleProxy()?source.schwarzschildRadius():0,WorldFeatures.gravityStrength);
        if(!field.supported() || base.distanceTo(new Vec3d(source.x(),source.y(),source.z()))>field.reach()+length)return null;
        return new Context(base,length,field);
    }
    public static Vec3d offset(float x,float y,float z,double t) {
        var c=current;if(c==null)return Vec3d.ZERO;
        var a=StringDeflection.offset(c.field,c.base.x+x,c.base.y+y,c.base.z+z,c.length,t);
        return new Vec3d(a.x(),a.y(),a.z());
    }
}
