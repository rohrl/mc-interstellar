// Static finite interior proxy, matched to the Schwarzschild exterior at BodyRadius.
// ds²=-A dt²+dr²/B+r²dOmega². See docs/gameplay-gravity.md; no stellar EOS is claimed.
uniform float BodyRadius;
float rayEnergySquared;
vec2 bodyMetric(float u) {
    float C=Radius/BodyRadius;
    if(u<=C)return vec2(1.0-u);
    float t=C*C*C/(u*u),d=1.0-C;
    return vec2(d*d/(1.0-.5*C-.5*t),1.0-t); // lapse A, inverse radial B
}
vec2 bodyDerivativeSide(vec2 q,bool interior) {
    float C=Radius/BodyRadius;
    if(!interior)return vec2(q.y,1.5*q.x*q.x-q.x);
    float t=C*C*C/(q.x*q.x),d=1.0-C;
    return vec2(q.y,rayEnergySquared*t*(3.0-C-2.0*t)/(2.0*q.x*d*d)-q.x);
}
vec2 bodyDerivative(vec2 q) {
    float C=Radius/BodyRadius;
    return bodyDerivativeSide(q,q.x>C || q.x==C && q.y>0.0);
}
vec2 bodyRkStep(vec2 q,float h,bool interior) {
    vec2 a=bodyDerivativeSide(q,interior),b=bodyDerivativeSide(q+h*a*.5,interior);
    vec2 c=bodyDerivativeSide(q+h*b*.5,interior),d=bodyDerivativeSide(q+h*c,interior);
    return q+h*(a+2.0*b+2.0*c+d)/6.0;
}
vec2 bodyAdvance(vec2 q,inout float h) {
    float C=Radius/BodyRadius;
    bool interior=q.x>C || q.x==C && q.y>0.0;
    vec2 next=bodyRkStep(q,h,interior);
    float surfaceSpeed2=rayEnergySquared-C*C*(1.0-C);
    // A grazing ray can enter and leave within one proposed step, with both
    // endpoints outside. Below the photon-sphere inverse radius, the exterior
    // potential is monotone: its turning point lies inside iff this is positive.
    bool grazing=!interior && C<=2.0/3.0 && q.y>0.0 && next.y<0.0 && surfaceSpeed2>=0.0;
    bool returning=q.x==C && (interior?next.x<C:next.x>C);
    // B is continuous at the surface but B' jumps. RK stages must stay on
    // one smooth branch; straddling it gives ray-dependent integration errors.
    if(returning || q.x!=C && ((q.x-C)*(next.x-C)<0.0 || grazing)) {
        if(surfaceSpeed2>=0.0) {
            float v=(returning?-1.0:1.0)*sign(q.y)*sqrt(surfaceSpeed2);
            // A grazing ray may leave again in its very next step. Seed the
            // nonzero return root, avoiding the entry root at angle zero.
            float crossing=clamp(returning?-2.0*q.y/bodyDerivativeSide(q,interior).y:2.0*(C-q.x)/(q.y+v),0.0,h);
            for(int i=0;i<3;i++) {
                vec2 at=bodyRkStep(q,crossing,interior);
                if(abs(at.y)>1e-8)crossing=clamp(crossing+(C-at.x)/at.y,0.0,h);
            }
            // The null first integral fixes the slope at the shared boundary.
            // Start the next chord there, using its outgoing side of the metric.
            h=crossing;next=vec2(C,v);
        }
    }
    return next;
}
