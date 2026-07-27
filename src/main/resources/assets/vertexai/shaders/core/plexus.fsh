#version 150

in vec4 vertexColor;
out vec4 fragColor;

uniform float GameTime;

// Hash function for random points
vec2 hash(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453);
}

void main() {
    // We scale FragCoord to create our grid.
    // 100.0 means each cell is 100x100 pixels.
    vec2 uv = gl_FragCoord.xy / 100.0;
    
    vec2 p = floor(uv);
    vec2 f = fract(uv);
    
    float col = 0.0;
    
    // Find our own point
    vec2 n = hash(p);
    vec2 p0 = 0.5 + 0.4 * sin((GameTime * 3000.0) + 6.2831 * n);
    
    // Draw dot for our cell
    float d0 = length(f - p0);
    col += smoothstep(0.06, 0.04, d0) * 1.5;
    
    // Check neighbors
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 g = vec2(float(x), float(y));
            vec2 n1 = hash(p + g);
            vec2 p1 = g + 0.5 + 0.4 * sin((GameTime * 3000.0) + 6.2831 * n1);
            
            // Distance from pixel to the neighbor's point
            float d = length(f - p1);
            col += smoothstep(0.06, 0.04, d); // Draw neighbor's dot
            
            // Distance from a point (f) to a line segment (p0 -> p1)
            vec2 pa = f - p0;
            vec2 ba = p1 - p0;
            float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
            float distToLine = length(pa - ba * h);
            
            // The distance between the points defines if we draw a line and how bright it is
            float pointDist = length(p1 - p0);
            if (pointDist < 1.6) {
                float lineAlpha = 1.0 - (pointDist / 1.6);
                float lineIntensity = smoothstep(0.03, 0.01, distToLine) * lineAlpha;
                col += lineIntensity;
            }
        }
    }
    
    // Background is dark blue
    vec3 bgColor = vec3(0.02, 0.04, 0.08); // roughly 0xFF050B14
    vec3 dotColor = vec3(0.3, 0.72, 1.0); // roughly 0xFF4DB8FF
    
    // Apply the plexus overlay to the background
    vec3 finalColor = mix(bgColor, dotColor, min(col, 1.0));
    
    fragColor = vec4(finalColor, 1.0) * vertexColor;
}
