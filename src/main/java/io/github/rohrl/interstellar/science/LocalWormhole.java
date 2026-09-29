package io.github.rohrl.interstellar.science;

import io.github.rohrl.interstellar.science.FiniteTerrainRay.Point;

/** Compact optical metric around two mouths in a shared exterior. The Cartesian
 * reference is diagnostic only; the GPU integrates three polar variables. */
public final class LocalWormhole {
    private LocalWormhole() {}
    public static double influence(double mouth,double separation) {
        if(!(mouth>0) || !(separation>mouth*2/.9))throw new IllegalArgumentException("Disjoint mouth regions required");
        return Math.min(12*mouth,.45*separation);
    }
    /** d(log n)/dr; n=1 outside, obtained by integrating this gradient inward.
     * Inner n differs from Ellis by a constant, preserving its spatial rays. */
    public static double gradient(double r,double mouth,double outer) {
        double t=Math.clamp((r-(mouth+outer)*.5)/((outer-mouth)*.5),0,1);
        return -2*mouth*mouth/(r*(r*r+mouth*mouth))*(1-t*t*(3-2*t));
    }
    public record Result(Point position,Point direction,int passages,boolean limited) {}
    private record State(Point p,Point d) {
        State add(State other,double h){return new State(p.add(other.p.scale(h)),d.add(other.d.scale(h)));}
    }
    private static State derivative(State q,Point centre,double mouth,double outer) {
        var offset=q.p.subtract(centre);double r=offset.length();var d=q.d.unit();
        var g=offset.scale(gradient(r,mouth,outer)/r);
        return new State(d,g.subtract(d.scale(d.dot(g))));
    }
    private static State step(State q,Point centre,double mouth,double outer,double h) {
        var a=derivative(q,centre,mouth,outer);var b=derivative(q.add(a,h/2),centre,mouth,outer);
        var c=derivative(q.add(b,h/2),centre,mouth,outer);var d=derivative(q.add(c,h),centre,mouth,outer);
        var result=q.add(a,h/6).add(b,h/3).add(c,h/3).add(d,h/6);
        return new State(result.p,result.d.unit());
    }
    private static Point reflect(Point p){return new Point(p.x(),p.y(),-p.z());}
    private static State transfer(State q,Point from,Point to,double mouth) {
        var offset=q.p.subtract(from);double rr=offset.dot(offset);
        return new State(to.add(reflect(offset).scale(mouth*mouth/rr)),
            reflect(q.d.subtract(offset.scale(2*offset.dot(q.d)/rr))).unit());
    }
    private static double entry(State q,Point centre,double outer) {
        var offset=q.p.subtract(centre);double r=offset.length(),along=offset.dot(q.d);
        if(r<outer-1e-7)return 0;
        if(along>=0)return Double.POSITIVE_INFINITY;
        double disc=along*along-offset.dot(offset)+outer*outer;
        return disc>0?Math.max(0,-along-Math.sqrt(disc)):Double.POSITIVE_INFINITY;
    }
    /** Fine Cartesian RK4 + bisection boundaries, independent of GPU polar RK4
     * and Newton boundary handling. No terrain; returns the final sky ray. */
    public static Result reference(Point a,Point b,Point eye,Point look,double mouth,double h) {
        double outer=influence(mouth,a.subtract(b).length());Point[] ends={a,b};
        var q=new State(eye,look.unit());int active=-1,passages=0;double angle=0;
        for(int end=0;end<2;end++)if(q.p.subtract(ends[end]).length()<mouth) {
            q=transfer(q,ends[end],ends[1-end],mouth);passages=1;break;
        }
        for(int iteration=0;iteration<200000;iteration++) {
            if(active<0) {
                double da=entry(q,a,outer),db=entry(q,b,outer),distance=Math.min(da,db);
                if(!Double.isFinite(distance))return new Result(q.p,q.d,passages,false);
                active=da<=db?0:1;q=new State(q.p.add(q.d.scale(distance)),q.d);
            }
            var centre=ends[active];var next=step(q,centre,mouth,outer,h);
            double r=next.p.subtract(centre).length();
            boolean crossing=r<mouth-1e-10,leaving=r>=outer && next.d.dot(next.p.subtract(centre))>0;
            if(crossing || leaving) {
                double boundary=crossing?mouth:outer,lo=0,hi=h;
                for(int root=0;root<35;root++) {
                    double middle=(lo+hi)/2;double radius=step(q,centre,mouth,outer,middle).p.subtract(centre).length();
                    if(crossing?radius>boundary:radius<boundary)lo=middle;else hi=middle;
                }
                next=step(q,centre,mouth,outer,(lo+hi)/2);
                next=new State(centre.add(next.p.subtract(centre).unit().scale(boundary)),next.d);
            }
            var n=q.p.subtract(centre).unit();double mu=n.dot(q.d);
            angle+=next.p.subtract(q.p).length()*Math.sqrt(Math.max(0,1-mu*mu))/q.p.subtract(centre).length();
            q=next;
            if(crossing) {
                if(passages>=4)return new Result(q.p,q.d,passages,true);
                q=transfer(q,centre,ends[1-active],mouth);active=1-active;passages++;
            } else if(leaving)active=-1;
            if(angle>8*Math.PI)return new Result(q.p,q.d,passages,true);
        }
        throw new IllegalStateException("Local reference exceeded step budget");
    }
}
