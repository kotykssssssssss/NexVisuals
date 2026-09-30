#version 330
layout(std140) uniform BackgroundConfig {
    vec4 Primary;
    vec4 Secondary;
    vec4 Accent;
    vec4 Shape;
    vec4 Grade;
    vec4 View;
};
in vec2 texCoord;
out vec4 fragColor;
float hash(vec2 p) { vec3 q=fract(vec3(p.xyx)*.1031); q+=dot(q,q.yzx+33.33); return fract((q.x+q.y)*q.z); }
float field(vec2 p) {
    vec2 i=floor(p),f=fract(p); f=f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+1.0),f.x),f.y);
}
void main() {
    vec2 uv=texCoord;
    vec2 p=(uv-.5)*vec2(View.x,1.0);
    float t=Shape.y*.12, move=Shape.z, soft=Shape.w;
    vec3 a=Primary.rgb*Primary.a,b=Secondary.rgb*Secondary.a,c=Accent.rgb*Accent.a;
    vec3 color=mix(b,a,uv.y)*.10;
    int style=int(Shape.x+.5);
    if(style==0) {
        // Broad horizontal curtains, slowly drifting independently from the fine light strands.
        float center=.50+sin(p.x*3.0+t*.5)*.13*move;
        float d=abs(uv.y-center);
        float band=exp(-d*d/(.004+soft*.016));
        float strands=.55+.45*sin(p.x*12.0+t*.7+sin(p.x*4.0)*2.0);
        color+=mix(a,b,.5+.5*sin(p.x*1.8+t*.3))*band*.55;
        color+=c*band*strands*.13;
    } else if(style==1) {
        vec2 first=vec2(-.28+sin(t*.4)*.15*move,.15+cos(t*.3)*.12*move);
        vec2 second=vec2(.4+cos(t*.35)*.15*move,-.12+sin(t*.2)*.14*move);
        color+=a*exp(-dot(p-first,p-first)/( .16+soft*.25))*.55;
        color+=b*exp(-dot(p-second,p-second)/(.12+soft*.22))*.55;
        color+=c*exp(-dot(p,p)/(.14+soft*.2))*.08;
    } else if(style==2) {
        vec2 q=p*3.0+vec2(t*.025,-t*.017)*move;
        float cloud=field(q)*.65+field(q*2.4)*.35;
        float mist=smoothstep(.3,.85,cloud)*exp(-p.y*p.y*3.0);
        color+=mix(b,a,cloud)*mist*.6+c*pow(mist,3.0)*.1;
    } else if(style==3 || style==5) {
        // Curved bands with a shaded body and thin bright rim; signature uses a diagonal layered path.
        float bend=style==3?smoothstep(.18,.82,uv.y):uv.y;
        float center=.22+.56*bend+sin(uv.y*4.0+t*.35)*.055*move;
        float d=(uv.x-center)*View.x;
        float width=style==3?.095:.11;
        float body=1.0-smoothstep(width,width+.02+soft*.04,abs(d));
        float ridge=exp(-pow((abs(d)-width)/(.002+soft*.01),2.0));
        float second=exp(-pow((d+.17+sin(t*.2)*.02*move)/(.035+soft*.07),2.0));
        color+=mix(a,b,smoothstep(-width,width,d))*body*.7;
        color+=b*second*.45+c*ridge*.25;
        if(style==5) color+=c*exp(-pow((d-.22)/(.035+soft*.09),2.0))*.08;
    } else {
        float slope=clamp(uv.x*.55+uv.y*.45+sin(t*.2)*.03*move,0.0,1.0);
        color=mix(b,a,slope)*.25+c*.025;
    }
    if(View.y>0.0) {
        // Bounded procedural grid: cost does not grow with the requested number of motes.
        vec2 grid=vec2(12,6),q=(uv+vec2(0,t*.006*move))*grid;
        vec2 cell=floor(q),f=fract(q);
        float seed=hash(cell);
        vec2 center=vec2(.15+.7*seed,.15+.7*hash(cell+17.0));
        vec2 delta=(f-center)/grid*vec2(View.x,1.0);
        float dotGlow=exp(-dot(delta,delta)/.00006);
        color+=c*dotGlow*step(seed,View.y/72.0)*(.045+.045*sin(t*.3+seed*6.28));
    }
    color=mix(vec3(.003,.006,.012),color,Grade.x)*Grade.y;
    float luma=dot(color,vec3(.2126,.7152,.0722));
    color=mix(vec3(luma),color,Grade.z);
    color*=1.0-Grade.w;
    fragColor=vec4(clamp(color,0.0,1.0),1.0);
}
