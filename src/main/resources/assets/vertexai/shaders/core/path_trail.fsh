#version 150

in vec4 vertexColor;
in vec3 worldPos;
out vec4 fragColor;

uniform vec4 ColorModulator;
uniform float GameTime;

void main() {
    float wave = sin((worldPos.x + worldPos.y + worldPos.z) * 1.5 - (GameTime * 8000.0)) * 0.5 + 0.5;
    vec3 baseColor = vertexColor.rgb;
    vec3 glowColor = mix(baseColor, vec3(1.0, 1.0, 1.0), wave * 0.35);
    float alpha = vertexColor.a * (0.85 + 0.15 * wave);
    
    fragColor = vec4(glowColor, alpha) * ColorModulator;
}
