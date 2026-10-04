package dev.nexvisuals.client.background;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.Identifier;
import java.util.OptionalInt;
import org.joml.Matrix3x2f;

/** Title: one direct triangle. Other menus: reusable half-size canvas submitted after queued HUD, before widgets. */
final class LiveBackgroundRenderer implements AutoCloseable {
    private static final RenderPipeline PIPELINE=RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(id("pipeline/live_background")).withVertexShader(id("core/live_background"))
            .withFragmentShader(id("core/live_background")).withUniform("BackgroundConfig",UniformType.UNIFORM_BUFFER).build();
    private final LiveBackgroundModule module;
    private MappableRingBuffer uniforms;
    private TextureTarget canvas;
    LiveBackgroundRenderer(LiveBackgroundModule module) { this.module=module; }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("nexvisuals",path); }
    void render(Minecraft client,GuiGraphics graphics,double phase,boolean queued) {
        if(!RenderSystem.getDevice().precompilePipeline(PIPELINE).isValid()) throw new IllegalStateException("Cannot compile NexVisuals live_background");
        if(uniforms==null) uniforms=new MappableRingBuffer(()->"NexVisuals background settings",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_MAP_WRITE,128);
        RenderTarget target=client.getMainRenderTarget();
        if(queued) {
            int width=Math.max(1,(target.width+1)/2),height=Math.max(1,(target.height+1)/2);
            if(canvas==null || canvas.width!=width || canvas.height!=height) {
                closeCanvas();
                canvas=new TextureTarget("NexVisuals menu wallpaper",width,height,false);
            }
            target=canvas;
        } else closeCanvas();
        var encoder=RenderSystem.getDevice().createCommandEncoder();
        try(var mapped=encoder.mapBuffer(uniforms.currentBuffer(),false,true)) {
            var u=Std140Builder.intoBuffer(mapped.data());
            color(u,module.primary.get()); color(u,module.secondary.get()); color(u,module.accent.get());
            u.putVec4(module.style.get().ordinal(),(float)(phase%36000),module.motion.get().floatValue(),module.softness.get().floatValue());
            u.putVec4(module.intensity.get().floatValue(),module.brightness.get().floatValue(),module.saturation.get().floatValue(),module.dim.get().floatValue());
            u.putVec4((float)target.width/Math.max(1,target.height),module.particles.get(),target.width,target.height);
            u.putVec4(module.moteSize.get().floatValue(),module.moteOpacity.get().floatValue(),module.moteSpeed.get().floatValue(),module.moteTwinkle.get().floatValue());
            color(u,module.moteAccent.get()?module.accent.get():module.moteColor.get());
        }
        try(var pass=encoder.createRenderPass(()->"NexVisuals live wallpaper",target.getColorTextureView(),OptionalInt.empty())) {
            pass.setPipeline(PIPELINE);
            pass.setUniform("BackgroundConfig",uniforms.currentBuffer());
            pass.draw(0,3);
        }
        uniforms.rotate();
        if(queued) {
            // GuiGraphics is extracted before batched HUD rendering. A direct main-target draw here would
            // leave queued hearts/hotbar on top of the wallpaper. Native blit ordering preserves layering.
            graphics.guiRenderState.submitGuiElement(new BlitRenderState(RenderPipelines.GUI_TEXTURED,
                    TextureSetup.singleTexture(canvas.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR)),
                    new Matrix3x2f(graphics.pose()),0,0,graphics.guiWidth(),graphics.guiHeight(),0,1,1,0,-1,graphics.scissorStack.peek()));
        }
    }
    private static void color(Std140Builder u,int c) { u.putVec4(((c>>>16)&255)/255f,((c>>>8)&255)/255f,(c&255)/255f,(c>>>24)/255f); }
    private void closeCanvas() { if(canvas!=null) { canvas.destroyBuffers(); canvas=null; } }
    @Override public void close() { closeCanvas(); if(uniforms!=null) { uniforms.close(); uniforms=null; } }
}
