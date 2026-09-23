package io.github.rohrl.interstellar.gravity;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import java.util.function.BooleanSupplier;

/** Reuse vanilla per-segment collisions, keeping full physical velocity for damage and deflection. */
public final class ProjectileStep {
    public boolean active;
    public double dt=1;
    public int index;
    public void run(Entity entity,Runnable tick,BooleanSupplier grounded) {
        if(!(entity.getWorld() instanceof ServerWorld world)) {tick.run();return;}
        boolean inGround=grounded.getAsBoolean();
        var field=GravitySources.find(world,entity.getPos(),inGround?entity.getPos():entity.getPos().add(entity.getVelocity()));
        if(field==null) {tick.run();return;}
        if(GravityMotion.captured(field,entity.getPos(),entity.getPos())) {entity.discard();return;}
        if(inGround) {tick.run();return;}
        int steps=field.strength()==0?1:Math.max(2,Math.min(16,(int)Math.ceil(entity.getVelocity().length()/.5)));
        double step=1.0/steps;active=true;
        try {
            for(index=0;index<steps&&!entity.isRemoved();index++) {
                dt=step;Vec3d start=entity.getPos();
                kick(entity,field,dt/2);
                Vec3d end=start.add(entity.getVelocity().multiply(dt));
                double capture=GravitySources.config.capture?field.captureFraction(start.x,start.y,start.z,end.x,end.y,end.z):-1;
                // Limit native hit queries to the horizon so targets behind it cannot be damaged.
                if(capture>=0)dt*=capture;
                if(dt>1e-10)tick.run();
                if(entity.isRemoved())break;
                Vec3d actual=entity.getPos();
                if(GravityMotion.captured(field,start,actual)||capture>=0&&actual.squaredDistanceTo(endAt(start,end,capture))<1e-12) {
                    entity.discard();break;
                }
                if(grounded.getAsBoolean())break;
                kick(entity,field,dt/2);
            }
            if(!entity.isRemoved())entity.velocityModified=true;
        } finally {active=false;dt=1;index=0;}
    }
    private static Vec3d endAt(Vec3d start,Vec3d end,double fraction) {return start.lerp(end,fraction);}
    private static void kick(Entity entity,GravityField field,double dt) {
        var p=entity.getPos();var a=field.acceleration(p.x,p.y,p.z);
        entity.setVelocity(entity.getVelocity().add(a.x()*dt,a.y()*dt,a.z()*dt));
    }
}
