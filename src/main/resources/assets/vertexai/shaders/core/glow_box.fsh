#version 150

in vec4 vertexColor;
in vec3 localPos;
out vec4 fragColor;

uniform vec4 ColorModulator;
uniform float GameTime;

void main() {
    float scanline = sin((localPos.y + GameTime * 2000.0) * 8.0) * 0.12 + 0.88;
    vec3 col = vertexColor.rgb * scanline;
    fragColor = vec4(col, vertexColor.a) * ColorModulator;
}
