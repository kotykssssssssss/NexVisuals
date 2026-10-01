package dev.nexvisuals.client.effect;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.nexvisuals.client.ClientModules;
import dev.nexvisuals.core.config.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Projection checks for the real module and vanilla 1.21.11 handheld asset; no game window or GPU. */
class ViewmodelPresetTest {
    @TempDir Path directory;

    private void preset(ViewmodelModule module,String name) {
        module.applyPreset(module.presets().stream().filter(p->p.name().equals(name)).findFirst().orElseThrow());
    }
    private Vector3f vector(JsonArray array,float divisor) {
        return new Vector3f(array.get(0).getAsFloat()/divisor,array.get(1).getAsFloat()/divisor,array.get(2).getAsFloat()/divisor);
    }
    private PoseStack heldPose(ViewmodelModule module,InteractionHand hand,HumanoidArm mainArm) throws Exception {
        var arm=hand==InteractionHand.MAIN_HAND?mainArm:mainArm.getOpposite();
        var pose=new PoseStack();module.apply(pose,hand,mainArm);
        // ItemInHandRenderer.applyItemArmTransform at rest, followed by the pack's actual display transform.
        pose.translate(arm==HumanoidArm.RIGHT?.56:-.56,-.52,-.72);
        try(var input=getClass().getResourceAsStream("/assets/minecraft/models/item/handheld.json")) {
            assertNotNull(input);
            var display=JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonObject("display").getAsJsonObject(arm==HumanoidArm.RIGHT?"firstperson_righthand":"firstperson_lefthand");
            new ItemTransform(vector(display.getAsJsonArray("rotation"),1),vector(display.getAsJsonArray("translation"),16),
                    vector(display.getAsJsonArray("scale"),1)).apply(arm==HumanoidArm.LEFT,pose.last());
        }
        return pose;
    }
    private int visibleBladeSamples(PoseStack pose,float fov,float aspect) {
        var projection=new Matrix4f().perspective((float)Math.toRadians(fov),aspect,.05f,100);
        var matrix=projection.mul(pose.last().pose());
        int visible=0;
        for(int i=0;i<=8;i++) {
            float coordinate=.22f+i*.085f;
            var point=matrix.transformProject(new Vector3f(coordinate,coordinate,.5f));
            if(Math.abs(point.x)<.95f && Math.abs(point.y)<.95f && point.z>=-1 && point.z<=1) visible++;
        }
        return visible;
    }
    @Test void correctedPresetsKeepTheHeldBladeInFrameForBothArmsAndCommonProjections() throws Exception {
        var module=new ViewmodelModule();module.setEnabled(true);
        for(String name:new String[]{"PvP","Cinematic"}) {
            preset(module,name);
            for(var arm:HumanoidArm.values()) for(var hand:InteractionHand.values())
                for(float fov:new float[]{55,64,70,85}) for(float aspect:new float[]{4f/3,16f/9,21f/9}) {
                    int visible=visibleBladeSamples(heldPose(module,hand,arm),fov,aspect);
                    assertTrue(visible>=5,name+" "+arm+" "+hand+" fov="+fov+" aspect="+aspect+" visible="+visible);
                }
        }
    }
    @Test void oldBrokenRecipesAreCaughtByTheProjectionCheck() throws Exception {
        var legacyRecipes=java.util.Map.of(
                "PvP","{\"main_x\":0.12,\"main_y\":-0.18,\"main_pitch\":-15,\"main_roll\":-22,\"main_scale\":0.85}",
                "Cinematic","{\"main_x\":0.2,\"main_yaw\":-28,\"main_roll\":12,\"main_scale\":1.12}");
        for(var recipe:legacyRecipes.entrySet()) {
            var old=JsonParser.parseString("{\"schemaVersion\":1,\"modules\":{\"viewmodel\":{\"enabled\":true,\"settings\":"+recipe.getValue()+"}}}").getAsJsonObject();
            var catalog=new ClientModules();new ConfigManager(directory.resolve("old.json"),catalog.registry,new GlobalSettings()).apply(old);
            assertTrue(visibleBladeSamples(heldPose(catalog.viewmodel,InteractionHand.MAIN_HAND,HumanoidArm.RIGHT),70,16f/9)<5,recipe.getKey());
            preset(catalog.viewmodel,recipe.getKey());
            assertTrue(visibleBladeSamples(heldPose(catalog.viewmodel,InteractionHand.MAIN_HAND,HumanoidArm.RIGHT),70,16f/9)>=5,recipe.getKey());
        }
    }
    @Test void mirroredLayoutIsOptInAndMirrorsTheGripWhenTheMainArmChanges() {
        var module=new ViewmodelModule();module.setEnabled(true);
        assertFalse((Boolean)module.settings().stream().filter(s->s.id().equals("mirror_layout")).findFirst().orElseThrow().get());
        preset(module,"PvP");
        var right=new PoseStack();var left=new PoseStack();
        module.apply(right,InteractionHand.MAIN_HAND,HumanoidArm.RIGHT);module.apply(left,InteractionHand.MAIN_HAND,HumanoidArm.LEFT);
        var r=right.last().pose().transformPosition(new Vector3f(.56f,-.52f,-.72f));
        var l=left.last().pose().transformPosition(new Vector3f(-.56f,-.52f,-.72f));
        assertEquals(-r.x,l.x,.00001);assertEquals(r.y,l.y,.00001);assertEquals(r.z,l.z,.00001);
    }
    @Test void globalCinematicUsesTheCorrectedRecipeAndBothPresetsPersist() throws Exception {
        var catalog=new ClientModules();var globals=new GlobalSettings();
        var config=new ConfigManager(directory.resolve("active.json"),catalog.registry,globals);
        var profiles=new ProfileManager(directory.resolve("profiles"),catalog.registry,globals);
        preset(catalog.viewmodel,"Cinematic");
        var expected=catalog.viewmodel.settings().stream().map(s->s.toJson().deepCopy()).toList();
        assertTrue(profiles.applyBuiltin(BuiltinProfile.CINEMATIC).isEmpty());
        assertEquals(expected,catalog.viewmodel.settings().stream().map(s->s.toJson()).toList());
        for(String name:new String[]{"PvP","Cinematic"}) {
            preset(catalog.viewmodel,name);catalog.viewmodel.setEnabled(true);config.save();
            var snapshot=config.snapshot();profiles.save(name);
            config.resetDefaults();assertTrue(profiles.load(name).warnings().isEmpty());assertEquals(snapshot,config.snapshot());
            var restored=new ClientModules();var next=new ConfigManager(config.path(),restored.registry,new GlobalSettings());
            assertTrue(next.load().warnings().isEmpty());assertEquals(snapshot,next.snapshot());
            assertEquals(name,restored.viewmodel.currentPresetName());
        }
    }
}
