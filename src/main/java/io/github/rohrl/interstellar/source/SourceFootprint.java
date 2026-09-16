package io.github.rohrl.interstellar.source;

/** Conservative horizontal bounds including the one-block connectivity shell. */
record SourceFootprint(int minX, int maxX, int minZ, int maxZ) {
    static SourceFootprint enclosing(double x, double z, double radius) {
        return new SourceFootprint(chunk(x-radius-1), chunk(x+radius+1),
                chunk(z-radius-1), chunk(z+radius+1));
    }
    private static int chunk(double coordinate) { return Math.floorDiv((int)Math.floor(coordinate),16); }
    boolean contains(int x, int z) { return x>=minX && x<=maxX && z>=minZ && z<=maxZ; }
    SourceFootprint including(int x,int z) {return new SourceFootprint(Math.min(minX,x),Math.max(maxX,x),Math.min(minZ,z),Math.max(maxZ,z));}
}
