package dev.nexvisuals.client.cosmetic;

import com.google.gson.JsonPrimitive;
import dev.nexvisuals.client.particle.EffectEmitter;
import java.lang.reflect.Proxy;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.impl.client.rendering.world.WorldExtractionContextImpl;
import net.fabricmc.fabric.impl.client.rendering.world.WorldRenderContextImpl;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import static org.junit.jupiter.api.Assertions.*;

/** Reproduces Fabric's first-frame empty context without creating Minecraft or GPU resources. */
class PlayerTrailsRenderTest {
    private PlayerTrailsModule ribbon() {
        var module=new PlayerTrailsModule(new EffectEmitter());
        module.settings().stream().filter(s->s.id().equals("style")).findFirst().orElseThrow().fromJson(new JsonPrimitive("RIBBON"));
        module.setEnabled(true);
        return module;
    }
    @Test void enabledRibbonSkipsTheActualUnpreparedFabricDrawContext() {
        var context=new WorldRenderContextImpl();
        assertNull(context.worldState());assertNull(context.matrices());assertNull(context.consumers());
        assertDoesNotThrow(()->ribbon().drawFrame(context));
    }
    @Test void emptyExtractionContextDoesNotTouchMinecraftOrPublishStaleData() {
        var context=new WorldExtractionContextImpl();
        assertNull(context.worldState());
        assertDoesNotThrow(()->ribbon().extractFrame(context));
    }
    @Test void disabledAndParticleModesDoNotReadDrawContext() {
        var forbidden=(WorldRenderContext)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{WorldRenderContext.class},
                (proxy,method,args)->{throw new AssertionError("Unexpected context access: "+method.getName());});
        var module=new PlayerTrailsModule(new EffectEmitter());
        assertDoesNotThrow(()->module.drawFrame(forbidden));
        module.setEnabled(true);
        assertDoesNotThrow(()->module.drawFrame(forbidden));
    }
    @Test void drawingIsRegisteredInTheMainPassInsteadOfTheUnpreparedDebugEvent() throws Exception {
        try(var input=getClass().getClassLoader().getResourceAsStream("dev/nexvisuals/client/cosmetic/PlayerTrailsModule.class")) {
            assertNotNull(input);var node=new ClassNode();new ClassReader(input).accept(node,0);
            var registration=node.methods.stream().filter(m->m.name.equals("registerRendering")).findFirst().orElseThrow();
            var events=new java.util.ArrayList<String>();
            for(var instruction:registration.instructions) if(instruction instanceof FieldInsnNode field
                    && field.getOpcode()==Opcodes.GETSTATIC && field.owner.endsWith("/WorldRenderEvents")) events.add(field.name);
            assertEquals(List.of("END_EXTRACTION","AFTER_ENTITIES"),events);
        }
    }
}
