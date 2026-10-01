package dev.nexvisuals.core.visual;

import dev.nexvisuals.core.animation.EffectMath;
import java.util.Arrays;

/** Immutable, bounded extraction snapshot. Native Minecraft buffers draw the colored quads. */
public final class RibbonMesh {
    public static final RibbonMesh EMPTY = new RibbonMesh(new float[0], new int[0]);
    private final float[] positions;
    private final int[] colors;
    private RibbonMesh(float[] positions,int[] colors) { this.positions=positions; this.colors=colors; }
    public int vertices() { return colors.length; }
    public float coordinate(int vertex,int axis) { return positions[vertex*3+axis]; }
    public int color(int vertex) { return colors[vertex]; }

    public static RibbonMesh player(TrailHistory path,long now,long lifetime,double width,double taper,
                                    int primary,int secondary,int quality,boolean dual,boolean fade,
                                    double cameraX,double cameraY,double cameraZ,
                                    double headX,double headY,double headZ) {
        if(path.size()<2 || now-path.time(path.size()-1)>=lifetime) return EMPTY;
        int steps=Math.clamp(quality,1,3), sections=(path.size()-1)*steps+1;
        Builder mesh=new Builder((sections-1)*12*(dual?2:1));
        double[] last=new double[6], current=new double[6], point=new double[3], tangent=new double[3];
        int lastColor=0;
        for(int section=0;section<sections;section++) {
            double at=section/(double)steps;
            sample(path,at,headX,headY,headZ,point);
            sample(path,Math.min(path.size()-1,at+.02),headX,headY,headZ,tangent);
            if(at>=path.size()-1) {
                sample(path,at-.02,headX,headY,headZ,tangent);
                for(int axis=0;axis<3;axis++) tangent[axis]=point[axis]-tangent[axis];
            } else for(int axis=0;axis<3;axis++) tangent[axis]-=point[axis];
            double vx=cameraX-point[0],vy=cameraY-point[1],vz=cameraZ-point[2];
            double sx=tangent[1]*vz-tangent[2]*vy,sy=tangent[2]*vx-tangent[0]*vz,sz=tangent[0]*vy-tangent[1]*vx;
            double norm=Math.sqrt(sx*sx+sy*sy+sz*sz);
            if(norm<1e-8) {sx=1;sy=sz=0;norm=1;}
            sx/=norm;sy/=norm;sz/=norm;
            if(section>0 && sx*last[3]+sy*last[4]+sz*last[5]<0) {sx=-sx;sy=-sy;sz=-sz;}
            int segment=Math.min(path.size()-2,(int)at);
            double fraction=at-segment;
            double stamp=path.time(segment)+(path.time(segment+1)-path.time(segment))*fraction;
            double age=EffectMath.unit((now-stamp)/Math.max(1.0,lifetime));
            double tail=Math.pow(1-age,taper);
            int color=alpha(EffectMath.color(primary,secondary,age),fade?(1-age)*(1-age):age>=1?0:1);
            current[0]=point[0]-cameraX;current[1]=point[1]-cameraY;current[2]=point[2]-cameraZ;
            current[3]=sx;current[4]=sy;current[5]=sz;
            if(section>0) {
                double previousAt=(section-1.0)/steps;
                int p=Math.min(path.size()-2,(int)previousAt);
                double oldStamp=path.time(p)+(path.time(p+1)-path.time(p))*(previousAt-p);
                double previousTail=Math.pow(1-EffectMath.unit((now-oldStamp)/Math.max(1.0,lifetime)),taper);
                if(dual) {
                    mesh.band(last,current,-width*.6,-width*.6,width*.22*previousTail,width*.22*tail,lastColor,color);
                    mesh.band(last,current,width*.6,width*.6,width*.22*previousTail,width*.22*tail,lastColor,color);
                } else mesh.band(last,current,0,0,width*.5*previousTail,width*.5*tail,lastColor,color);
            }
            System.arraycopy(current,0,last,0,6);lastColor=color;
        }
        return mesh.finish();
    }

    public static RibbonMesh sweep(TrailHistory path,long now,long lifetime,int primary,int secondary) {
        return sweep(path,now,lifetime,primary,secondary,1);
    }
    public static RibbonMesh sweep(TrailHistory path,long now,long lifetime,int primary,int secondary,int quality) {
        if(path.size()<2 || lifetime<=0 || now-path.time(path.size()-1)>=lifetime) return EMPTY;
        int steps=Math.clamp(quality,1,3), sections=(path.size()-1)*steps;
        Builder mesh=new Builder(sections*12);
        double[] before=new double[6],after=new double[6];
        sweepSample(path,0,before);
        for(int i=1;i<=sections;i++) {
            double at=i/(double)steps, previous=(i-1)/(double)steps;
            sweepSample(path,at,after);
            double oldAge=EffectMath.unit((now-sweepTime(path,previous))/(double)lifetime), age=EffectMath.unit((now-sweepTime(path,at))/(double)lifetime);
            int a=alpha(EffectMath.color(primary,secondary,oldAge),Math.pow(1-oldAge,2));
            int b=alpha(EffectMath.color(primary,secondary,age),Math.pow(1-age,2));
            for(int strip=0;strip<3;strip++) {
                double low=switch(strip){case 0->0;case 1->.15;default->.85;};
                double high=switch(strip){case 0->.15;case 1->.85;default->1;};
                mesh.edge(before,low,strip==0?alpha(a,0):a);
                mesh.edge(after,low,strip==0?alpha(b,0):b);
                mesh.edge(after,high,strip==2?alpha(b,0):b);
                mesh.edge(before,high,strip==2?alpha(a,0):a);
            }
            double[] swap=before;before=after;after=swap;
        }
        return mesh.finish();
    }
    private static double sweepTime(TrailHistory path,double at) {
        int i=Math.min(path.size()-2,(int)at);double t=EffectMath.unit(at-i);
        return path.time(i)+(path.time(i+1)-path.time(i))*t;
    }
    private static void sweepSample(TrailHistory path,double at,double[] output) {
        int i=Math.min(path.size()-2,(int)at);double t=EffectMath.unit(at-i);
        for(int axis=0;axis<6;axis++) {
            double p0=path.coordinate(Math.max(0,i-1),axis),p1=path.coordinate(i,axis);
            double p2=path.coordinate(i+1,axis),p3=path.coordinate(Math.min(path.size()-1,i+2),axis);
            double value=.5*(2*p1+(-p0+p2)*t+(2*p0-5*p1+4*p2-p3)*t*t+(-p0+3*p1-3*p2+p3)*t*t*t);
            // Never overshoot a sampled blade edge into the near plane or an unrelated hand pose.
            output[axis]=Math.clamp(value,Math.min(p1,p2),Math.max(p1,p2));
        }
    }
    private static void sample(TrailHistory path,double at,double hx,double hy,double hz,double[] output) {
        int i=Math.min(path.size()-2,Math.max(0,(int)at));double t=EffectMath.unit(at-i);
        for(int axis=0;axis<3;axis++) {
            double p0=value(path,Math.max(0,i-1),axis,hx,hy,hz),p1=value(path,i,axis,hx,hy,hz);
            double p2=value(path,i+1,axis,hx,hy,hz),p3=value(path,Math.min(path.size()-1,i+2),axis,hx,hy,hz);
            output[axis]=.5*((2*p1)+(-p0+p2)*t+(2*p0-5*p1+4*p2-p3)*t*t+(-p0+3*p1-3*p2+p3)*t*t*t);
        }
    }
    private static double value(TrailHistory path,int i,int axis,double hx,double hy,double hz) {
        return i==path.size()-1?axis==0?hx:axis==1?hy:hz:path.coordinate(i,axis);
    }
    private static int alpha(int color,double multiplier) {return color&0xFFFFFF | Math.clamp((int)Math.round((color>>>24)*multiplier),0,255)<<24;}
    private static final class Builder {
        private static final double[] FRACTIONS={-1,-.65,.65,1};
        private final float[] positions;private final int[] colors;private int count;
        Builder(int vertices) { positions=new float[vertices*3];colors=new int[vertices]; }
        void vertex(double x,double y,double z,int color) {
            positions[count*3]=(float)x;positions[count*3+1]=(float)y;positions[count*3+2]=(float)z;colors[count++]=color;
        }
        void edge(double[] section,double across,int color) {
            vertex(section[0]+(section[3]-section[0])*across,
                    section[1]+(section[4]-section[1])*across,
                    section[2]+(section[5]-section[2])*across,color);
        }
        void corner(double[] section,double offset,int color) {vertex(section[0]+section[3]*offset,section[1]+section[4]*offset,section[2]+section[5]*offset,color);}
        void band(double[] a,double[] b,double centerA,double centerB,double widthA,double widthB,int ca,int cb) {
            for(int i=0;i<3;i++) {
                corner(a,centerA+FRACTIONS[i]*widthA,i==0?alpha(ca,0):ca);
                corner(b,centerB+FRACTIONS[i]*widthB,i==0?alpha(cb,0):cb);
                corner(b,centerB+FRACTIONS[i+1]*widthB,i==2?alpha(cb,0):cb);
                corner(a,centerA+FRACTIONS[i+1]*widthA,i==2?alpha(ca,0):ca);
            }
        }
        RibbonMesh finish() {return count==0?EMPTY:new RibbonMesh(count==colors.length?positions:Arrays.copyOf(positions,count*3),count==colors.length?colors:Arrays.copyOf(colors,count));}
    }
}
