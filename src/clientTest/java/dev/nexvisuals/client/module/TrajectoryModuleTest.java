package dev.nexvisuals.client.module;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParser;
import dev.nexvisuals.client.ClientModules;
import dev.nexvisuals.core.config.*;
import dev.nexvisuals.core.visual.TrajectoryMath;
import net.fabricmc.fabric.impl.client.rendering.world.WorldExtractionContextImpl;
import net.fabricmc.fabric.impl.client.rendering.world.WorldRenderContextImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TrajectoryModuleTest {
    @TempDir Path directory;
    @Test void catalogRegistrationPresetsSettingsRestartAndProfileRoundTrip() throws Exception {
        var catalog=new ClientModules(); var module=catalog.trajectory;
        assertSame(module,catalog.registry.find("local_trajectory").orElseThrow()); assertFalse(module.enabled());
        var globals=new GlobalSettings(); var config=new ConfigManager(directory.resolve("active.json"),catalog.registry,globals);
        var recipes=new HashSet<com.google.gson.JsonObject>();
        for (var preset:module.presets()) {
            module.applyPreset(preset); module.setEnabled(true); config.save(); var snapshot=config.snapshot();
            assertTrue(recipes.add(snapshot.getAsJsonObject("modules").getAsJsonObject(module.id())));
            var restored=new ClientModules(); var next=new ConfigManager(config.path(),restored.registry,new GlobalSettings());
            assertTrue(next.load().warnings().isEmpty()); assertEquals(snapshot,next.snapshot());
        }
        assertEquals(3,recipes.size());
        module.settings().stream().filter(s->s.id().equals("prediction_ticks")).findFirst().orElseThrow().fromJson(new JsonPrimitive(999));
        assertEquals(120,module.settings().stream().filter(s->s.id().equals("prediction_ticks")).findFirst().orElseThrow().get());
        var profiles=new ProfileManager(directory.resolve("profiles"),catalog.registry,globals);
        profiles.save("Guide"); var snapshot=config.snapshot(); profiles.restoreDefaults();
        assertFalse(module.enabled());
        assertTrue(profiles.load("Guide").warnings().isEmpty()); assertEquals(snapshot,config.snapshot());
    }
    @Test void oldConfigDoesNotEnableOrReconfigureTheNewModule() {
        var catalog=new ClientModules(); var config=new ConfigManager(directory.resolve("old.json"),catalog.registry,new GlobalSettings());
        assertTrue(config.apply(JsonParser.parseString("{\"schemaVersion\":1,\"modules\":{\"viewmodel\":{\"enabled\":true}}}").getAsJsonObject()).isEmpty());
        assertFalse(catalog.trajectory.enabled()); assertTrue(catalog.viewmodel.enabled());
        catalog.trajectory.settings().forEach(s->assertEquals(s.defaultValue(),s.get()));
    }
    @Test void disabledModuleAndActualFirstFrameEmptyFabricContextsAreSafe() {
        var module=new TrajectoryModule();
        assertDoesNotThrow(()->module.tick(null));
        module.setEnabled(true);
        assertDoesNotThrow(()->module.extractFrame(new WorldExtractionContextImpl()));
        assertDoesNotThrow(()->module.drawFrame(new WorldRenderContextImpl()));
        module.setEnabled(false);
        assertDoesNotThrow(()->module.tick(null));
    }
    @Test void predictionUsesBlockQueriesWithoutEntityScansOrSpawningAndDrawUsesOnlySnapshot() throws Exception {
        var node=read("dev/nexvisuals/client/module/TrajectoryModule");
        var events=new ArrayList<String>();
        for (var method:node.methods) for (var instruction:method.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                assertFalse(Set.of("getEntities","getEntitiesOfClass","getEntity","send","sendPacket","spawnProjectile","shoot","shootFromRotation").contains(call.name),call.name);
                if (method.name.equals("drawFrame")) assertFalse(call.owner.startsWith("net/minecraft/world/"),call.owner);
            }
            if (method.name.equals("registerRendering") && instruction instanceof FieldInsnNode field && field.owner.endsWith("/WorldRenderEvents")) events.add(field.name);
        }
        assertEquals(List.of("END_EXTRACTION","AFTER_ENTITIES"),events);
        var tick=node.methods.stream().filter(m->m.name.equals("tick")).findFirst().orElseThrow();
        assertTrue(Arrays.stream(tick.instructions.toArray()).anyMatch(n->n instanceof FieldInsnNode f && f.owner.equals("net/minecraft/client/Minecraft") && f.name.equals("player")));
    }
    @Test void integrationOrderAndGravityMatchThePinnedNativeMinecraftBytecode() throws Exception {
        var throwable=read("net/minecraft/world/entity/projectile/ThrowableProjectile");
        var tick=throwable.methods.stream().filter(m->m.name.equals("tick")).findFirst().orElseThrow();
        var calls=calls(tick);
        assertTrue(calls.indexOf("applyGravity") < calls.indexOf("applyInertia"));
        assertTrue(calls.indexOf("applyInertia") < calls.indexOf("getHitResultOnMoveVector"));
        var gravity=throwable.methods.stream().filter(m->m.name.equals("getDefaultGravity")).findFirst().orElseThrow();
        assertTrue(Arrays.stream(gravity.instructions.toArray()).anyMatch(n->n instanceof LdcInsnNode l && Double.valueOf(TrajectoryMath.Physics.THROWABLE.gravity).equals(l.cst)));
        var arrowTick=read("net/minecraft/world/entity/projectile/arrow/AbstractArrow").methods.stream().filter(m->m.name.equals("tick")).findFirst().orElseThrow();
        var arrowCalls=calls(arrowTick);
        assertTrue(arrowCalls.indexOf("stepMoveAndHit") < arrowCalls.lastIndexOf("applyInertia"));
        assertTrue(arrowCalls.lastIndexOf("applyInertia") < arrowCalls.indexOf("applyGravity"));
    }
    @Test void pointBillboardsFaceCameraAtHorizontalAndVerticalAnglesWithoutDegenerating() {
        for (double[] center:new double[][]{{0,0,10},{0,10,0},{0,-10,0},{3,4,5}}) {
            var vertices=new ArrayList<double[]>();
            var out=(com.mojang.blaze3d.vertex.VertexConsumer)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{com.mojang.blaze3d.vertex.VertexConsumer.class},(proxy,method,args)-> {
                        if (method.getName().equals("addVertex")) vertices.add(new double[]{((Number)args[1]).doubleValue(),((Number)args[2]).doubleValue(),((Number)args[3]).doubleValue()});
                        return proxy;
                    });
            TrajectoryModule.point(out,new org.joml.Matrix4f(),center[0],center[1],center[2],.1,0xFFFFFFFF);
            assertEquals(4,vertices.size());
            double[] a=vertices.get(0), b=vertices.get(1), d=vertices.get(3);
            double[] right={b[0]-a[0],b[1]-a[1],b[2]-a[2]}, up={d[0]-a[0],d[1]-a[1],d[2]-a[2]};
            assertEquals(.04,dot(right,right),1e-6); assertEquals(.04,dot(up,up),1e-6);
            assertEquals(0,dot(right,up),1e-6); assertEquals(0,dot(right,center),1e-5); assertEquals(0,dot(up,center),1e-5);
        }
    }
    private double dot(double[] a,double[] b) { return a[0]*b[0]+a[1]*b[1]+a[2]*b[2]; }
    private ClassNode read(String name) throws Exception {
        try (var stream=getClass().getClassLoader().getResourceAsStream(name+".class")) {
            assertNotNull(stream,name); var node=new ClassNode(); new ClassReader(stream).accept(node,0); return node;
        }
    }
    private List<String> calls(MethodNode method) {
        var calls=new ArrayList<String>();
        for (var instruction:method.instructions) if(instruction instanceof MethodInsnNode call) calls.add(call.name);
        return calls;
    }
}
