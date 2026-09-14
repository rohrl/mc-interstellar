#version 150
uniform vec2 Viewport;
uniform float CameraRadius;
uniform float Lensing;
uniform float Grid;
uniform float Aligned;
in vec2 screenUv;
out vec4 fragColor;
const float PI = 3.14159265359;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

// Illustrative emitting sky at infinity. Colours are not a spectral transport model.
vec3 sky(vec3 n) {
    float longitude = atan(n.x, n.z);
    float latitude = asin(clamp(n.y, -1.0, 1.0));
    vec2 uv = vec2(longitude / (2.0 * PI) + .5, latitude / PI + .5);
    vec3 color = mix(vec3(.012, .025, .065), vec3(.08, .025, .09), uv.y);
    float band = exp(-pow((n.y + .25 * n.x) * 7.0, 2.0));
    color += band * vec3(.05, .065, .10);
    vec2 cells = uv * vec2(420.0, 210.0);
    vec2 cell = floor(cells);
    vec2 offset = vec2(hash(cell), hash(cell + 31.7)) * .6 + .2;
    float star = 1.0 - smoothstep(.018, .065, length(fract(cells) - offset));
    color += star * step(.84, hash(cell + 7.3)) * vec3(.8, .9, 1.0);
    if (Grid > .5) {
        vec2 lines = abs(fract(uv * vec2(24.0, 12.0) + .5) - .5);
        float line = 1.0 - smoothstep(.009, .025, min(lines.x, lines.y));
        color += line * vec3(.12, .28, .36);
    }
    vec3 source = normalize(vec3(Aligned > .5 ? 0.0 : .25, .0, -1.0));
    float angularDistance = acos(clamp(dot(n, source), -1.0, 1.0));
    color += (1.0 - smoothstep(.022, .03, angularDistance)) * vec3(1.0, .72, .28);
    return color;
}

vec2 derivative(vec2 q) { return vec2(q.y, 1.5 * q.x * q.x - q.x); }

void main() {
    vec2 xy = (screenUv * 2.0 - 1.0) * .7002075382; // vertical FOV 70 degrees
    xy.x *= Viewport.x / Viewport.y;
    vec3 direction = normalize(vec3(xy.x, -xy.y, -1.0));
    if (Lensing < .5) { fragColor = vec4(sky(direction), 1.0); return; }
    float radial = direction.z;
    float tangent = length(direction.xy);
    if (tangent < 1e-6) { fragColor = vec4(0, 0, 0, 1); return; }
    vec3 e = vec3(direction.xy / tangent, 0);
    float u = 1.0 / CameraRadius;
    vec2 q = vec2(u, -radial * u * sqrt(1.0 - u) / tangent);
    float phi = 0.0;
    const float h = .02;
    for (int i = 0; i < 800; ++i) {
        vec2 a = derivative(q);
        vec2 b = derivative(q + h * a * .5);
        vec2 c = derivative(q + h * b * .5);
        vec2 d = derivative(q + h * c);
        vec2 next = q + h * (a + 2.0 * b + 2.0 * c + d) / 6.0;
        if (next.x >= 1.0) { fragColor = vec4(0, 0, 0, 1); return; }
        if (next.x <= 0.0) {
            float exitPhi = phi + h * q.x / (q.x - next.x);
            vec3 n = vec3(0, 0, cos(exitPhi)) + e * sin(exitPhi);
            fragColor = vec4(sky(normalize(n)), 1.0);
            return;
        }
        q = next;
        phi += h;
    }
    // Finite integration budget: show the limitation instead of inventing an image.
    fragColor = vec4(.5, .02, .3, 1);
}
