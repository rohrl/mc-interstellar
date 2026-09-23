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
vec2 bodyDerivative(vec2 q) {
    float C=Radius/BodyRadius;
    if(q.x<=C)return vec2(q.y,1.5*q.x*q.x-q.x);
    float t=C*C*C/(q.x*q.x),d=1.0-C;
    return vec2(q.y,rayEnergySquared*t*(3.0-C-2.0*t)/(2.0*q.x*d*d)-q.x);
}
