#version 150
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform float LensX;
uniform float LensY;
uniform float LensRadius;
uniform float LensDepth;
uniform float Aspect;
uniform float DiskAxisX;
uniform float DiskAxisY;
uniform float DiskInclination;
uniform float DiskSide;
uniform float OrbitTime;
uniform vec2 OutSize;
in vec2 texCoord;
out vec4 fragColor;

const float PI=3.14159265359;
float gaussian(float x) {return exp(-x*x);}
vec2 safeUV(vec2 uv) {return clamp(uv,vec2(0.001),vec2(0.999));}
float behindHole(vec2 uv) {return step(LensDepth-0.000005,texture(DepthSampler,safeUV(uv)).r);}
vec3 scene(vec2 uv,vec2 fallback) {
    float onScreen=step(0.001,uv.x)*step(uv.x,0.999)*step(0.001,uv.y)*step(uv.y,0.999);
    return texture(DiffuseSampler,mix(fallback,safeUV(uv),onScreen*behindHole(uv))).rgb;
}
// Visible bands sampled from Planck's law; exposure is artistic, not spectral radiometry.
vec3 thermal(float kelvin) {
    vec3 wavelengths=vec3(650.0,550.0,450.0);
    vec3 exponent=vec3(1.438777e7)/(wavelengths*max(kelvin,900.0));
    vec3 spectrum=pow(vec3(550.0)/wavelengths,vec3(5.0))/(exp(min(exponent,vec3(70.0)))-1.0);
    return spectrum/max(max(spectrum.r,spectrum.g),max(spectrum.b,0.000001));
}
// Schwarzschild reference: shadow radius ~2.598 Rs, disk inner edge ~3 Rs.
// g combines gravitational and Doppler frequency shifts; bolometric intensity scales as g^4.
vec3 emission(float diskRadius,float azimuth,float intensity) {
    if(intensity<0.00001) return vec3(0.0);
    float rsRadius=max(3.015,diskRadius*2.598);
    float beta=sqrt(1.0/(2.0*(rsRadius-1.0)));
    float sinI=sqrt(max(0.0,1.0-DiskInclination*DiskInclination));
    float doppler=sqrt(1.0-beta*beta)/(1.0+beta*sinI*cos(azimuth));
    float gravitational=sqrt(1.0-1.0/rsRadius);
    float g=gravitational*doppler;
    float temperature=8500.0*pow(3.015/rsRadius,0.75);
    float sheared=azimuth-OrbitTime*1.2/pow(max(diskRadius,1.0),1.5);
    float filaments=0.76+0.16*sin(sheared*19.0+diskRadius*29.0)+0.08*sin(sheared*37.0-diskRadius*51.0);
    return thermal(temperature*g)*min(3.2,pow(g,4.0))*filaments*intensity;
}
void main() {
    vec2 aspect=vec2(Aspect,1.0);
    vec2 center=vec2(LensX,LensY);
    vec2 offset=(texCoord-center)*aspect;
    float radius=max(LensRadius,0.0001);
    float rho=length(offset)/radius;
    float visible=behindHole(texCoord);
    vec3 original=texture(DiffuseSampler,texCoord).rgb;
    if(rho>4.8 || visible<0.5) {fragColor=vec4(original,1.0);return;}
    vec2 direction=offset/max(length(offset),0.00001);
    float annulus=smoothstep(1.0,1.055,rho)*(1.0-smoothstep(2.6,4.8,rho));
    // Qualitative lens equation with parity reversal. Hidden scenery is unavailable
    // to screen-space optics; remap only available background around the shadow.
    float beta=rho-3.0625/max(rho,0.15);
    float parity=smoothstep(-0.08,0.08,-beta)*PI;
    vec2 refracted=vec2(direction.x*cos(parity)-direction.y*sin(parity),direction.x*sin(parity)+direction.y*cos(parity));
    vec2 primary=center+refracted*radius*(1.13+abs(beta)*0.9)/aspect;
    vec3 color=mix(original,scene(primary,texCoord),annulus);
    float secondary=gaussian((rho-1.17)/0.1)*0.5*annulus;
    vec2 secondUV=center-direction*radius*(1.45+2.2*(rho-1.03))/aspect;
    color=mix(color,scene(secondUV,texCoord),secondary);
    // Gravity bends colors together. Redshift acts on the emitting material below.
    vec2 major=normalize(vec2(DiskAxisX,DiskAxisY));
    vec2 minor=vec2(-major.y,major.x);
    vec2 p=vec2(dot(offset,major),dot(offset,minor))/radius;
    float inclination=clamp(DiskInclination,0.08,1.0);
    float sinI=sqrt(max(0.0,1.0-inclination*inclination));
    vec2 inDisk=vec2(p.x,p.y/inclination);
    float diskR=length(inDisk);
    float azimuth=atan(inDisk.y,inDisk.x+0.000001);
    float diskMask=smoothstep(1.15,1.24,diskR)*(1.0-smoothstep(2.4,3.3,diskR));
    float inFront=1.0-smoothstep(-0.025,0.025,p.y*DiskSide);
    diskMask*=max(inFront,smoothstep(1.0,1.04,rho));
    float diskBrightness=1.6*pow(1.2/max(diskR,1.2),1.8);
    vec3 glow=emission(diskR,azimuth,diskBrightness*diskMask);
    // Lensed far-side disk above the shadow and a thinner underside image below.
    // Both humps vanish face-on; the world-space disk plane remains fixed.
    float upperShape=sqrt(max(0.0,2.56-p.x*p.x));
    float upperY=DiskSide*(0.18*inclination+upperShape*(0.7+0.3*sinI));
    float upper=gaussian((p.y-upperY)/(0.055+0.09*sinI));
    upper*=(1.0-smoothstep(1.45,1.62,abs(p.x)))*sinI*sinI*smoothstep(1.0,1.04,rho);
    float lowerY=-DiskSide*sqrt(max(0.0,1.21-p.x*p.x));
    float lower=gaussian((p.y-lowerY)/0.035)*(1.0-smoothstep(1.0,1.12,abs(p.x)));
    lower*=sinI*sinI*smoothstep(1.0,1.02,rho);
    glow+=emission(1.65,atan(upperShape,p.x),1.1*upper);
    glow+=emission(1.24,atan(-upperShape,p.x),0.45*lower);
    // Successively narrower and fainter images approaching the shadow edge.
    float pixel=1.0/max(OutSize.y*radius,1.0);
    float ring1=gaussian((rho-1.105)/max(0.018,pixel));
    float ring2=gaussian((rho-1.052)/max(0.008,pixel*.65));
    float ring3=gaussian((rho-1.024)/max(0.003,pixel*.4));
    glow+=emission(1.3,atan(p.y,p.x+0.000001),ring1*.75+ring2*.26+ring3*.09);
    float halo=gaussian((rho-1.14)/0.24)*0.09;
    glow+=emission(1.45,atan(p.y,p.x+0.000001),halo);
    color*=1.0-0.42*diskMask;
    color+=glow;
    fragColor=vec4(color,1.0);
}
