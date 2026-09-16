package io.github.rohrl.interstellar.client;

import org.joml.Matrix4f;

/** Perspective ray slopes from the actual world frame, including dynamic FOV and zoom. */
record WorldProjection(float x, float y, float offsetX, float offsetY) {
    private static WorldProjection current;
    static void capture(Matrix4f projection) {
        float x=1/projection.m00(),y=1/projection.m11();
        current=new WorldProjection(x,y,projection.m20()*x,projection.m21()*y);
    }
    static WorldProjection current() {
        if(current==null)throw new IllegalStateException("Waiting for world projection");
        return current;
    }
    double verticalFov() {return Math.toDegrees(Math.atan(offsetY+y)-Math.atan(offsetY-y));}
}
