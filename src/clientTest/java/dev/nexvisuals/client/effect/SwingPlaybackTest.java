package dev.nexvisuals.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises actual module playback with explicit clocks, without booting Minecraft or its graphics. */
class SwingPlaybackTest {
    private Matrix4f matrix(SwingModule module,HumanoidArm arm,long now) {
        var pose=new PoseStack();module.applyAt(pose,arm,now);return new Matrix4f(pose.last().pose());
    }
    @Test void everyPresetPlaysFromOneAcceptedClickEvenWithoutFurtherNativeProgress() {
        var module=new SwingModule();module.setEnabled(true);
        for(var preset:module.presets()) {
            module.setEnabled(false);module.setEnabled(true);module.applyPreset(preset);
            module.start(HumanoidArm.RIGHT,1,0);
            if(preset.name().equals("VANILLA")) { assertEquals(new Matrix4f(),matrix(module,HumanoidArm.RIGHT,70_000_000));continue; }
            assertFalse(new Matrix4f().equals(matrix(module,HumanoidArm.RIGHT,70_000_000),.001f),preset.name());
            assertEquals(new Matrix4f(),matrix(module,HumanoidArm.LEFT,70_000_000));
            assertTrue(new Matrix4f().equals(matrix(module,HumanoidArm.RIGHT,950_000_000),.00001f),preset.name()+" settles");
        }
    }
    @Test void retriggerHasNoPoseJumpAndTurningOffRestoresNativeImmediately() {
        var module=new SwingModule();module.setEnabled(true);module.applyPreset(module.presets().get(3));
        module.start(HumanoidArm.RIGHT,1,0);
        var before=matrix(module,HumanoidArm.RIGHT,140_000_000);
        module.start(HumanoidArm.RIGHT,4,140_000_000);
        assertTrue(before.equals(matrix(module,HumanoidArm.RIGHT,140_000_000),.00001f));
        module.start(HumanoidArm.RIGHT,4,180_000_000);
        assertFalse(before.equals(matrix(module,HumanoidArm.RIGHT,180_000_000),.00001f),"same-tick duplicate doesn't freeze motion");
        module.setEnabled(false);assertFalse(module.applyAt(new PoseStack(),HumanoidArm.RIGHT,190_000_000));
        module.setEnabled(true);assertEquals(new Matrix4f(),matrix(module,HumanoidArm.RIGHT,200_000_000));
    }
}
