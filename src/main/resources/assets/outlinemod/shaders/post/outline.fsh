#version 150

uniform sampler2D In;
uniform sampler2D InDepth;

uniform vec2 InSize;
uniform vec4 OutlineColor;
uniform float OutlineThickness;
uniform float DepthThreshold;

in vec2 texCoord;
out vec4 fragColor;

float sampleDepth(vec2 uv) {
    return texture(InDepth, uv).r;
}

float detectEdge(vec2 uv, vec2 texel) {
    float t  = OutlineThickness;

    // Ambil 8 tetangga sekitar pixel
    float tl = sampleDepth(uv + texel * vec2(-t, -t));
    float tm = sampleDepth(uv + texel * vec2( 0, -t));
    float tr = sampleDepth(uv + texel * vec2( t, -t));
    float ml = sampleDepth(uv + texel * vec2(-t,  0));
    float mr = sampleDepth(uv + texel * vec2( t,  0));
    float bl = sampleDepth(uv + texel * vec2(-t,  t));
    float bm = sampleDepth(uv + texel * vec2( 0,  t));
    float br = sampleDepth(uv + texel * vec2( t,  t));

    // Sobel X dan Y
    float gx = -tl - 2.0*ml - bl + tr + 2.0*mr + br;
    float gy = -tl - 2.0*tm - tr + bl + 2.0*bm + br;

    return sqrt(gx*gx + gy*gy);
}

void main() {
    vec2 texel = 1.0 / InSize;
    vec4 scene = texture(In, texCoord);

    float edge = detectEdge(texCoord, texel);
    float mask = step(DepthThreshold, edge);

    // Campurkan warna outline di atas scene
    fragColor = mix(scene, vec4(OutlineColor.rgb, 1.0), mask * OutlineColor.a);
}
