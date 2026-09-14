#version 150
in vec3 Position;


uniform vec2 Viewport;
out vec2 screenUv;
void main() {
    screenUv = Position.xy / Viewport;
    // Own the full viewport; GUI projection rounds scaled dimensions after resizing.
    gl_Position = vec4(screenUv * vec2(2.0, -2.0) + vec2(-1.0, 1.0), 0.0, 1.0);
}
