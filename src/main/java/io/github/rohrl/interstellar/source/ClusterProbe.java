package io.github.rohrl.interstellar.source;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Function;

/** Incremental six-neighbour inspection. The caller supplies a stable, non-loading view. */
public final class ClusterProbe {
    public static final double RADIUS_PER_BLOCK = Math.sqrt(3)/32;
    public static final double LEGACY_RADIUS_PER_BLOCK = .125;
    public record Cell(int x, int y, int z) { }
    public enum CellState { MASS, EMPTY, UNKNOWN }
    public enum Status { COMPLETE, PARTIAL, LIMIT, EMPTY }
    public record Result(Status status, int count, double x, double y, double z,
                         double enclosingRadius, double schwarzschildRadius) {
        public double compactness() { return enclosingRadius == 0 ? 0 : schwarzschildRadius/enclosingRadius; }
        public boolean blackHoleProxy() { return status == Status.COMPLETE && count > 0 && compactness() >= 1-1e-12; }
    }
    private static final int[][] DIRECTIONS = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private final Function<Cell,CellState> view;
    private final int limit;
    private final double radiusPerBlock;
    private final ArrayDeque<Cell> queue = new ArrayDeque<>();
    private final HashSet<Cell> seen = new HashSet<>();
    private final ArrayList<Cell> mass = new ArrayList<>();
    private boolean unknown;
    private Result result;

    public ClusterProbe(Cell seed, Function<Cell,CellState> view, int limit) {
        this(seed,view,limit,RADIUS_PER_BLOCK);
    }
    public ClusterProbe(Cell seed, Function<Cell,CellState> view, int limit, double radiusPerBlock) {
        if (limit < 1 || limit > 4096) throw new IllegalArgumentException("Probe limit must be 1..4096");
        if(!Double.isFinite(radiusPerBlock)||radiusPerBlock<=0)throw new IllegalArgumentException("Positive finite mass required");
        this.view = view;
        this.limit = limit;
        this.radiusPerBlock=radiusPerBlock;
        seen.add(seed); queue.add(seed);
    }
    public boolean finished() { return result != null; }
    public boolean touches(Cell cell) {return seen.contains(cell);}
    public java.util.List<Cell> members() {
        if(!finished())throw new IllegalStateException("Inspection is still running");
        return java.util.List.copyOf(mass);
    }
    public Result result() {
        if (!finished()) throw new IllegalStateException("Inspection is still running");
        return result;
    }
    /** At most budget cell queries; final geometry reduction is bounded by limit <= 4096. */
    public int advance(int budget) {
        if (budget < 1) throw new IllegalArgumentException("Positive budget required");
        int reads = 0;
        while (result == null && !queue.isEmpty() && reads < budget) {
            Cell cell = queue.remove();
            CellState state = view.apply(cell);
            reads++;
            if (state == CellState.UNKNOWN) { unknown = true; continue; }
            if (state != CellState.MASS) continue;
            if (mass.size() == limit) { finish(Status.LIMIT); break; }
            mass.add(cell);
            for (int[] d : DIRECTIONS) {
                Cell adjacent = new Cell(cell.x+d[0],cell.y+d[1],cell.z+d[2]);
                if (seen.add(adjacent)) queue.add(adjacent);
            }
        }
        if (result == null && queue.isEmpty()) finish(unknown ? Status.PARTIAL : mass.isEmpty() ? Status.EMPTY : Status.COMPLETE);
        return reads;
    }
    private void finish(Status status) {
        double x = 0, y = 0, z = 0, radiusSquared = 0;
        for (Cell c : mass) { x += c.x+.5; y += c.y+.5; z += c.z+.5; }
        int count = mass.size();
        if (count > 0) { x /= count; y /= count; z /= count; }
        for (Cell c : mass) {
            double dx = Math.abs(c.x+.5-x)+.5, dy = Math.abs(c.y+.5-y)+.5, dz = Math.abs(c.z+.5-z)+.5;
            radiusSquared = Math.max(radiusSquared,dx*dx+dy*dy+dz*dz);
        }
        result = new Result(status,count,x,y,z,Math.sqrt(radiusSquared),count*radiusPerBlock);
        queue.clear(); seen.clear();
    }
}
