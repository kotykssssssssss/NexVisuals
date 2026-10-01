package dev.nexvisuals.core.animation;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwingMotionTest {
    private SwingMotion.Pose pose(SwingMotion.Style style,double progress,double amplitude) {
        var output=new SwingMotion.Pose();
        SwingMotion.sample(style,progress,.35,Easing.OUT_CUBIC,amplitude,-.35,.12,-.2,-40,30,-60,1.1,output);
        return output;
    }
    @Test void everyCustomStyleMovesOnASingleClickAndReturnsToNeutral() {
        var distinct=new HashSet<String>();
        for(var style:SwingMotion.Style.values()) {
            if(style==SwingMotion.Style.VANILLA) continue;
            var middle=pose(style,.25,1);
            assertTrue(Math.abs(middle.x)+Math.abs(middle.y)+Math.abs(middle.z)+Math.abs(middle.pitch)+Math.abs(middle.roll)>0,style.toString());
            assertTrue(distinct.add(middle.x+":"+middle.y+":"+middle.z+":"+middle.pitch+":"+middle.yaw+":"+middle.roll));
            for(double p:new double[]{0,1,2}) {
                var end=pose(style,p,1);assertEquals(0,end.x);assertEquals(0,end.y);assertEquals(0,end.z);
                assertEquals(0,end.pitch);assertEquals(0,end.yaw);assertEquals(0,end.roll);assertEquals(1,end.scale);
            }
        }
    }
    @Test void noAnimationAtZeroAmplitudeAndFiniteBoundedScale() {
        for(var style:SwingMotion.Style.values()) for(int i=0;i<=100;i++) {
            var quiet=pose(style,i/100.0,0);assertEquals(0,quiet.x);assertEquals(0,quiet.roll);assertEquals(1,quiet.scale);
            var strong=pose(style,i/100.0,2);assertTrue(Double.isFinite(strong.roll));assertTrue(strong.scale>=.1 && strong.scale<=3);
        }
    }
    @Test void retriggerCarriesTheCurrentPoseAndReleasesItWithin65Milliseconds() {
        var previous=pose(SwingMotion.Style.SLASH,.4,1);var carry=new SwingMotion.Pose();carry.copy(previous);
        var next=pose(SwingMotion.Style.SLASH,0,1);next.carry(carry,SwingMotion.restartCarry(0));
        assertEquals(previous.x,next.x);assertEquals(previous.pitch,next.pitch);assertEquals(previous.roll,next.roll);
        assertEquals(0,SwingMotion.restartCarry(65_000_000));assertEquals(0,SwingMotion.restartCarry(100_000_000));
        var spin=pose(SwingMotion.Style.SPIN,.98,1);carry.copy(spin);
        assertTrue(Math.abs(carry.roll)<2,"A near-complete spin must not carry a second 360-degree turn");
    }
    @Test void sampleDensityCannotChangeThePoseAtAGivenTime() {
        SwingTimeline timeline=new SwingTimeline();timeline.start(1,0);
        var direct=pose(SwingMotion.Style.SWIPE,timeline.progress(180_000_000,300),1);
        for(long now=0;now<180_000_000;now+=1_000_000) pose(SwingMotion.Style.SWIPE,timeline.progress(now,300),1);
        var sampled=pose(SwingMotion.Style.SWIPE,timeline.progress(180_000_000,300),1);
        assertEquals(direct.x,sampled.x);assertEquals(direct.y,sampled.y);assertEquals(direct.roll,sampled.roll);
    }
}
