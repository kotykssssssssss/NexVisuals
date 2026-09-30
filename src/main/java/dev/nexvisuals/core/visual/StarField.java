package dev.nexvisuals.core.visual;

import java.util.Random;

/** Deterministic unit-sphere positions, rebuilt only when star geometry settings change. */
public final class StarField {
    public static final int MAX_STARS=4000;
    private StarField() { }
    public static float[] generate(int count) {
        count=Math.clamp(count,0,MAX_STARS);
        float[] data=new float[count*5];
        Random random=new Random(0x4E455856L);
        for(int i=0;i<count;i++) {
            double y=random.nextDouble()*2-1, angle=random.nextDouble()*Math.PI*2, radius=Math.sqrt(1-y*y);
            int p=i*5;
            data[p]=(float)(radius*Math.cos(angle)); data[p+1]=(float)y; data[p+2]=(float)(radius*Math.sin(angle));
            data[p+3]=random.nextFloat(); data[p+4]=.55f+random.nextFloat()*.45f;
        }
        return data;
    }
    public static double twinkle(double seconds, double speed, double phase, double intensity) {
        return 1-Math.clamp(intensity,0,1)*(.5+.5*Math.sin(seconds*speed+phase*Math.PI*2));
    }
}
