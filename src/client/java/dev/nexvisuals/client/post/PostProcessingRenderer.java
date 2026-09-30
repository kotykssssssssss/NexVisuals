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

/** One reused color target, one copy and one fullscreen triangle. No readback, depth changes or effect chain. */
public final class PostProcessingRenderer implements AutoCloseable {
    private static final RenderPipeline PIPELINE=RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(id("pipeline/visual_grade")).withVertexShader(id("core/visual_grade")).withFragmentShader(id("core/visual_grade"))
            .withSampler("SceneSampler").withUniform("VisualConfig",UniformType.UNIFORM_BUFFER).build();
    private final PostProcessingModule module;
    private TextureTarget copy;
    private MappableRingBuffer uniforms;
    public PostProcessingRenderer(PostProcessingModule module) { this.module=module; }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("nexvisuals",path); }
    public void render(Minecraft client,DeltaTracker delta) {
        // Validate before copying/drawing; a missing resource cannot replace the image with an invalid pass.
        if(!RenderSystem.getDevice().precompilePipeline(PIPELINE).isValid()) throw new IllegalStateException("Cannot compile NexVisuals visual_grade");
        RenderTarget target=client.getMainRenderTarget();
        if(copy==null || copy.width!=target.width || copy.height!=target.height) {
            if(copy!=null) copy.destroyBuffers();
            copy=new TextureTarget("NexVisuals image copy",target.width,target.height,false);
        }
        if(uniforms==null) uniforms=new MappableRingBuffer(()->"NexVisuals post settings",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_MAP_WRITE,144);
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
        }
        encoder.copyTextureToTexture(target.getColorTexture(),copy.getColorTexture(),0,0,0,0,0,target.width,target.height);
        try(RenderPass pass=encoder.createRenderPass(()->"NexVisuals visual grading",target.getColorTextureView(),OptionalInt.empty())) {
            pass.setPipeline(PIPELINE);
            pass.setUniform("VisualConfig",uniforms.currentBuffer());
            pass.bindTexture("SceneSampler",copy.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR));
            pass.draw(0,3);
        }
        uniforms.rotate();
    }
    private static float f(dev.nexvisuals.core.setting.DoubleSetting setting) { return setting.get().floatValue(); }
    private static void color(Std140Builder u,int color,float strength) {
        u.putVec4(((color>>>16)&255)/255f,((color>>>8)&255)/255f,(color&255)/255f,(color>>>24)/255f*strength);
    }
    @Override public void close() {
        if(copy!=null) { copy.destroyBuffers(); copy=null; }
        if(uniforms!=null) { uniforms.close(); uniforms=null; }
    }
}
