#version 150
uniform sampler2D DiffuseSampler;
uniform float Mono;
uniform float Flash;
uniform float Blackout;
uniform float Shock;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 radial=texCoord-0.5;
    vec2 warped=clamp(texCoord+radial*sin(length(radial)*35.0)*Shock,vec2(0.001),vec2(0.999));
    vec3 original=texture(DiffuseSampler,warped).rgb;
    float luminance=dot(original,vec3(0.2126,0.7152,0.0722));
    float impact=clamp((luminance-0.42)*1.9+0.5,0.0,1.0);
    vec3 color=mix(original,vec3(impact),Mono);
    float vignette=smoothstep(0.18,0.72,length(texCoord-0.5));
    color*=1.0-vignette*Mono*0.25;
    color=mix(color,vec3(1.0,0.985,0.97),Flash);
    color*=1.0-Blackout;
    fragColor=vec4(color,1.0);
}
