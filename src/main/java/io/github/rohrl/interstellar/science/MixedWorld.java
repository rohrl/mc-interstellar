package io.github.rohrl.interstellar.science;

import io.github.rohrl.interstellar.science.FiniteTerrainRay.Point;

/** Diagnostic reference for the explicitly approximate shared-world composition.
 * Uses metric derivatives for mass acceleration and fine midpoint integration,
 * independently of the GPU's simplified coefficient and adaptive RK4. */
public record MixedWorld(Point a,Point b,Point mass,double radius,double body) {
    private static final Point ZERO=new Point(0,0,0);
    public double extent(){return Math.max(96,Math.max(48*radius,8*body));}
    public double influence(){return LocalWormhole.influence(8,a.subtract(b).length());}
    public static double window(double r,double outer) {double t=Math.clamp(2*r/outer-1,0,1);return 1-t*t*(3-2*t);}
    public static double coefficient(double r,double mu,double rs,double body) {
        double s2=Math.max(0,1-mu*mu);
        if(body<=0 || r>=body)return -1.5*rs*s2/(r*r);
        double c=rs/body,t=rs*r*r/(body*body*body),b=1-t;
        return -rs*r*(mu*mu/b+s2)*(3-c-2*t)/(2*body*body*body*(1-.5*c-.5*t));
    }
    public static Point affineAcceleration(Point p,Point d,double rs,double body) {
        double r=p.length();if(r<1e-8 || rs==0)return ZERO;
        var n=p.scale(1/r);double mu=n.dot(d),aa,bb,ap,bp;
        if(body>0) {var m=new ExtendedSourceMetric(rs,body);aa=m.lapse(r);bb=m.inverseRadial(r);ap=m.lapseDerivative(r);bp=m.inverseRadialDerivative(r);}
        else {aa=bb=1-rs/r;ap=bp=rs/(r*r);}
        // The areal-coordinate Hamiltonian is singular at the horizon, although
        // the spatial curve is regular. Reference capture tests stop there.
        if(body==0 && r<rs*1.001)return n.subtract(d.scale(mu)).scale(coefficient(r,mu,rs,0));
        double pr=mu/bb,e2=aa*(1-mu*mu+mu*mu/bb);
        var momentum=d.add(n.scale((1/bb-1)*mu));
        var pdot=n.scale(-.5*(e2*ap/(aa*aa)+bp*pr*pr)).subtract(momentum.subtract(n.scale(pr)).scale((bb-1)*pr/r));
        var ndot=d.subtract(n.scale(mu)).scale(1/r);
        double prdot=pdot.dot(n)+momentum.dot(ndot);
        var acceleration=pdot.add(n.scale(bp*mu*pr+(bb-1)*prdot)).add(ndot.scale((bb-1)*pr));
        return acceleration.subtract(d.scale(acceleration.dot(d)));
    }
    public Point acceleration(Point p,Point d) {
        var offset=p.subtract(mass);var result=affineAcceleration(offset,d,radius,body).scale(window(offset.length(),extent()));
        for(var centre:new Point[]{a,b}) {
            offset=p.subtract(centre);double r=offset.length();var n=offset.scale(1/Math.max(1e-8,r));
            result=result.add(n.subtract(d.scale(n.dot(d))).scale(LocalWormhole.gradient(Math.max(r,1e-8),8,influence())));
        }
        return result;
    }
    private record State(Point p,Point d) {}
    private State step(State q,double h) {
        var middleD=q.d.add(acceleration(q.p,q.d).scale(h*.5)).unit();
        var middleP=q.p.add(q.d.scale(h*.5));
        return new State(q.p.add(middleD.scale(h)),q.d.add(acceleration(middleP,middleD).scale(h)).unit());
    }
    private static double entry(State q,Point centre,double size) {
        var v=q.p.subtract(centre);double along=v.dot(q.d),c=v.dot(v)-size*size;
        if(along>=0)return Double.POSITIVE_INFINITY;if(c<0)return 0;
        double disc=along*along-c;return disc>0?Math.max(0,-along-Math.sqrt(disc)):Double.POSITIVE_INFINITY;
    }
    private static Point reflect(Point p){return new Point(p.x(),p.y(),-p.z());}
    private State transfer(State q,Point from,Point to) {
        var offset=q.p.subtract(from);double rr=offset.dot(offset);
        return new State(to.add(reflect(offset).scale(64/rr)),reflect(q.d.subtract(offset.scale(2*offset.dot(q.d)/rr))).unit());
    }
    public record Result(Point direction,int code) {}
    /** Exterior-observer fixture; near/interior horizon frames keep their separate reference. */
    public Result reference(Point eye,Point look,double h) {
        var q=new State(eye,look.unit());int passages=0;double angle=0;
        Point[] mouths={a,b};
        for(int i=0;i<2;i++)if(q.p.subtract(mouths[i]).length()<8){q=transfer(q,mouths[i],mouths[1-i]);passages++;break;}
        if(radius>0) {
            var v=q.p.subtract(mass);double r=v.length(),radial;
            if(body==0 && r<=radius*1.25)throw new IllegalArgumentException("Use horizon observer fixtures for this camera");
            radial=body>0?new ExtendedSourceMetric(radius,body).inverseRadial(r):1-radius/r;
            var n=v.scale(1/r);double mu=q.d.dot(n),factor=Math.sqrt(1+(radial-1)*window(r,extent()));
            q=new State(q.p,q.d.add(n.scale(mu*(factor-1))).unit());
        }
        for(int iteration=0;iteration<200000;iteration++) {
            boolean outside=(radius==0 || q.p.subtract(mass).length()>extent()+1e-7)
                && q.p.subtract(a).length()>influence()+1e-7 && q.p.subtract(b).length()>influence()+1e-7;
            if(outside) {
                double distance=Math.min(entry(q,a,influence()),entry(q,b,influence()));
                if(radius>0)distance=Math.min(distance,entry(q,mass,extent()));
                if(!Double.isFinite(distance))return new Result(q.d,passages);
                q=new State(q.p.add(q.d.scale(distance)),q.d);
            }
            // The finite body's radial metric derivative jumps at its surface.
            // Resolve that boundary much more finely than the ordinary path.
            double stepSize=body>0?Math.min(h,Math.max(.00005,.2*Math.abs(q.p.subtract(mass).length()-body))):h;
            var next=step(q,stepSize);int contact=-1;double time=stepSize;
            for(int i=0;i<3;i++) {
                if(i==2 && (radius==0 || body>0))continue;
                var centre=i==2?mass:mouths[i];double bound=i==2?radius:8;
                if(next.p.subtract(centre).length()>=bound)continue;
                double lo=0,hi=stepSize;
                for(int j=0;j<30;j++) {double mid=(lo+hi)/2;if(step(q,mid).p.subtract(centre).length()>bound)lo=mid;else hi=mid;}
                if(hi<=time){time=hi;contact=i;}
            }
            if(contact>=0)next=step(q,time);
            angle+=time*acceleration(q.p,q.d).length();q=next;
            if(contact==2)return new Result(ZERO,-1);
            if(contact>=0) {
                if(passages>=4)return new Result(ZERO,-3);
                q=transfer(q,mouths[contact],mouths[1-contact]);passages++;
            }
            if(angle>8*Math.PI)return new Result(ZERO,-3);
        }
        throw new IllegalStateException("Mixed reference step budget");
    }
}
