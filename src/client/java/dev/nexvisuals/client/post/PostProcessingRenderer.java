package dev.nexvisuals.client.post;

import com.mojang.blaze3d.buffers.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.attribute.EnvironmentAttributes;
import java.util.OptionalInt;

/** Reused scene copy and optional quarter-size highlights target. No readback or depth changes. */
public final class PostProcessingRenderer implements AutoCloseable {
    private static final RenderPipeline PIPELINE=RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(id("pipeline/visual_grade")).withVertexShader(id("core/visual_grade")).withFragmentShader(id("core/visual_grade"))
            .withSampler("SceneSampler").withSampler("GlowSampler").withUniform("VisualConfig",UniformType.UNIFORM_BUFFER).build();
    private static final RenderPipeline EXTRACT=RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(id("pipeline/glow_extract")).withVertexShader(id("core/glow_extract")).withFragmentShader(id("core/glow_extract"))
            .withSampler("SceneSampler").withUniform("HighlightConfig",UniformType.UNIFORM_BUFFER).build();
    private final PostProcessingModule module;
    private TextureTarget copy;
    private TextureTarget glow;
    private MappableRingBuffer uniforms;
    private MappableRingBuffer highlightUniforms;
    public PostProcessingRenderer(PostProcessingModule module) { this.module=module; }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("nexvisuals",path); }
    public void render(Minecraft client,DeltaTracker delta) {
        // Validate before copying/drawing; a missing resource cannot replace the image with an invalid pass.
        if(!RenderSystem.getDevice().precompilePipeline(PIPELINE).isValid()) throw new IllegalStateException("Cannot compile NexVisuals visual_grade");
        boolean wide=module.bloom.get() && module.glowFootprint.get()==PostProcessingModule.GlowFootprint.WIDE;
        if(wide && !RenderSystem.getDevice().precompilePipeline(EXTRACT).isValid()) throw new IllegalStateException("Cannot compile NexVisuals glow_extract");
        RenderTarget target=client.getMainRenderTarget();
        if(copy==null || copy.width!=target.width || copy.height!=target.height) {
            if(copy!=null) copy.destroyBuffers();
            copy=new TextureTarget("NexVisuals image copy",target.width,target.height,false);
        }
        if(uniforms==null) uniforms=new MappableRingBuffer(()->"NexVisuals post settings",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_MAP_WRITE,176);
        float partial=delta.getGameTimeDeltaPartialTick(false);
        float night=Math.clamp(client.gameRenderer.getMainCamera().attributeProbe().getValue(EnvironmentAttributes.STAR_BRIGHTNESS,partial)*2,0,1);
        float hurt=Math.clamp((client.player.hurtTime-partial)/Math.max(1f,client.player.hurtDuration),0,1);
        float seconds=(client.level.getGameTime()%720_000+partial)/20f;
        CommandEncoder encoder=RenderSystem.getDevice().createCommandEncoder();
        try(var mapped=encoder.mapBuffer(uniforms.currentBuffer(),false,true)) {
            var u=Std140Builder.intoBuffer(mapped.data());
            boolean grading=module.grading.get();
            u.putVec4(grading?f(module.brightness):1,grading?f(module.contrast):1,grading?f(module.saturation):1,grading?f(module.gamma):1);
            u.putVec4(grading?f(module.temperature):0,grading?f(module.tint):0,f(module.intensity),seconds);
            u.putVec4(module.vignette.get()?f(module.vignetteIntensity):0,f(module.vignetteRadius),f(module.vignetteSoftness),module.grain.get()?f(module.grainIntensity):0);
            u.putVec4(module.chromatic.get()?f(module.chromaticAmount):0,module.bloom.get()?f(module.bloomIntensity):0,f(module.bloomRadius),f(module.bloomThreshold));
            color(u,module.filterColor.get(),module.filter.get()?f(module.filterIntensity):0);
            color(u,module.nightColor.get(),module.nightTint.get()?f(module.nightIntensity)*night:0);
            color(u,module.damageColor.get(),module.damage.get()?f(module.damageIntensity)*hurt:0);
            color(u,module.vignetteColor.get(),1);
            u.putVec4(target.width,target.height,0,0);
            u.putVec4(module.edgeSoftness.get()?f(module.edgeIntensity):0,f(module.edgeRadius),module.glowFootprint.get().ordinal(),0);
            u.putVec4(grading?f(module.exposure):0,grading?f(module.highlights):0,f(module.grainScale),0);
        }
        encoder.copyTextureToTexture(target.getColorTexture(),copy.getColorTexture(),0,0,0,0,0,target.width,target.height);
        if(wide) extractHighlights(encoder); else closeGlow();
        try(RenderPass pass=encoder.createRenderPass(()->"NexVisuals visual grading",target.getColorTextureView(),OptionalInt.empty())) {
            pass.setPipeline(PIPELINE);
            pass.setUniform("VisualConfig",uniforms.currentBuffer());
            pass.bindTexture("SceneSampler",copy.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR));
            pass.bindTexture("GlowSampler",wide?glow.getColorTextureView():copy.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR));
            pass.draw(0,3);
        }
        uniforms.rotate();
    }
    private void extractHighlights(CommandEncoder encoder) {
        int width=Math.max(1,(copy.width+3)/4),height=Math.max(1,(copy.height+3)/4);
        if(glow==null || glow.width!=width || glow.height!=height) {
            if(glow!=null) glow.destroyBuffers();
            glow=new TextureTarget("NexVisuals quarter highlights",width,height,false);
        }
        if(highlightUniforms==null) highlightUniforms=new MappableRingBuffer(()->"NexVisuals highlight settings",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_MAP_WRITE,16);
        try(var mapped=encoder.mapBuffer(highlightUniforms.currentBuffer(),false,true)) {
            Std140Builder.intoBuffer(mapped.data()).putVec4(copy.width,copy.height,f(module.bloomThreshold),0);
        }
        try(var pass=encoder.createRenderPass(()->"NexVisuals selective highlights",glow.getColorTextureView(),OptionalInt.empty())) {
            pass.setPipeline(EXTRACT);
            pass.setUniform("HighlightConfig",highlightUniforms.currentBuffer());
            pass.bindTexture("SceneSampler",copy.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR));
            pass.draw(0,3);
        }
        highlightUniforms.rotate();
    }
    private void closeGlow() {
        if(glow!=null) { glow.destroyBuffers(); glow=null; }
        if(highlightUniforms!=null) { highlightUniforms.close(); highlightUniforms=null; }
    }
    private static float f(dev.nexvisuals.core.setting.DoubleSetting setting) { return setting.get().floatValue(); }
    private static void color(Std140Builder u,int color,float strength) {
        u.putVec4(((color>>>16)&255)/255f,((color>>>8)&255)/255f,(color&255)/255f,(color>>>24)/255f*strength);
    }
    @Override public void close() {
        closeGlow();
        if(copy!=null) { copy.destroyBuffers(); copy=null; }
        if(uniforms!=null) { uniforms.close(); uniforms=null; }
    }
}
