package dev.nexvisuals.client.sky;

import com.mojang.blaze3d.buffers.*;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.vertex.*;
import dev.nexvisuals.core.visual.StarField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/** Two bounded meshes, uploaded on creation/edit. All animation happens in small GPU uniforms. */
public final class SkyboxRenderer implements AutoCloseable {
    // Optional pipelines compile on demand, so an unsupported shader cannot abort Minecraft's resource reload.
    private static final RenderPipeline DOME=RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(id("pipeline/sky_dome")).withVertexShader(id("core/sky_dome")).withFragmentShader(id("core/sky_dome"))
            .withUniform("SkyConfig",UniformType.UNIFORM_BUFFER).withVertexFormat(DefaultVertexFormat.POSITION,VertexFormat.Mode.QUADS)
            .withDepthWrite(false).withCull(false).build();
    private static final RenderPipeline STARS=RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/custom_stars")).withVertexShader(id("core/custom_stars")).withFragmentShader(id("core/custom_stars"))
            .withUniform("StarConfig",UniformType.UNIFORM_BUFFER).withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS)
            .withBlend(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT).withDepthWrite(false).withCull(false).build();
    private final SkyboxModule module;
    private GpuBuffer dome,stars;
    private MappableRingBuffer skyUniforms,starUniforms;
    private int domeIndices,starIndices,cachedCount=-1;
    private double cachedSize=-1;
    private final Matrix4f identity=new Matrix4f();
    private final Vector3f zero=new Vector3f();
    private final Vector4f white=new Vector4f(1);
    private final Matrix4f starMatrix=new Matrix4f();
    public SkyboxRenderer(SkyboxModule module) { this.module=module; }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("nexvisuals",path); }
    private static void valid(RenderPipeline pipeline) {
        if(!RenderSystem.getDevice().precompilePipeline(pipeline).isValid()) throw new IllegalStateException("Cannot compile "+pipeline.getLocation());
    }
    public void dome(SkyboxModule.Frame frame) {
        valid(DOME);
        if(dome==null) {
            int segments=32,bands=16;
            try(var storage=ByteBufferBuilder.exactlySized(segments*bands*4*DefaultVertexFormat.POSITION.getVertexSize())) {
                var builder=new BufferBuilder(storage,VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);
                for(int lat=0;lat<bands;lat++) for(int lon=0;lon<segments;lon++) {
                    vertex(builder,lat,lon,bands,segments); vertex(builder,lat+1,lon,bands,segments);
                    vertex(builder,lat+1,lon+1,bands,segments); vertex(builder,lat,lon+1,bands,segments);
                }
                try(MeshData mesh=builder.buildOrThrow()) {
                    domeIndices=mesh.drawState().indexCount();
                    dome=RenderSystem.getDevice().createBuffer(()->"NexVisuals sky dome",GpuBuffer.USAGE_VERTEX,mesh.vertexBuffer());
                }
            }
            skyUniforms=new MappableRingBuffer(()->"NexVisuals sky settings",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_MAP_WRITE,176);
        }
        var encoder=RenderSystem.getDevice().createCommandEncoder();
        try(var view=encoder.mapBuffer(skyUniforms.currentBuffer(),false,true)) {
            var u=Std140Builder.intoBuffer(view.data());
            rgb(u,frame.sky(),1); rgb(u,frame.horizon(),1); rgb(u,frame.zenith(),1);
            u.putVec4(module.gradient.get()?module.gradientIntensity.get().floatValue():0,module.horizonHeight.get().floatValue(),module.horizonSoftness.get().floatValue(),(float)frame.seconds());
            u.putVec4((float)frame.night(),frame.rain(),module.nebula.get()?module.nebulaIntensity.get().floatValue():0,module.aurora.get()?module.auroraIntensity.get().floatValue():0);
            rgb(u,module.nebulaColor.get(),1); rgb(u,module.auroraColor.get(),1);
            u.putVec4(module.glow.get()?module.glowIntensity.get().floatValue():0,module.shootingStars.get()?module.meteorInterval.get().floatValue():0,module.atmosphereSpeed.get().floatValue(),(float)frame.sunset());
            rgb(u,module.glowColor.get(),1);
            u.putVec4(module.haze.get().floatValue()*(float)frame.day(),module.sunHalo.get().floatValue()*(1-(float)frame.night()),frame.sunAngle(),0);
            rgb(u,module.sunTint.get(),module.sunOpacity.get().floatValue());
        }
        draw(DOME,dome,domeIndices,skyUniforms.currentBuffer(),"SkyConfig",RenderSystem.getModelViewMatrix());
        skyUniforms.rotate();
    }
    private static void vertex(BufferBuilder builder,int lat,int lon,int bands,int segments) {
        double elevation=-Math.PI/2+Math.PI*lat/bands,azimuth=2*Math.PI*lon/segments;
        builder.addVertex((float)(100*Math.cos(elevation)*Math.cos(azimuth)),(float)(100*Math.sin(elevation)),(float)(100*Math.cos(elevation)*Math.sin(azimuth)));
    }
    public void stars(SkyboxModule.Frame frame,PoseStack poses) {
        if(module.starAmount.get()==0 || frame.starVisibility()<=0) return;
        valid(STARS);
        if(stars==null || cachedCount!=module.starAmount.get() || cachedSize!=module.starSize.get()) rebuildStars();
        if(starUniforms==null) starUniforms=new MappableRingBuffer(()->"NexVisuals star settings",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_MAP_WRITE,48);
        var encoder=RenderSystem.getDevice().createCommandEncoder();
        try(var view=encoder.mapBuffer(starUniforms.currentBuffer(),false,true)) {
            var u=Std140Builder.intoBuffer(view.data());
            rgb(u,module.starColor.get(),module.starOpacity.get().floatValue());
            u.putVec4((float)frame.seconds(),frame.starVisibility()*module.starBrightness.get().floatValue(),module.twinkleSpeed.get().floatValue(),module.twinkle.get()?module.twinkleIntensity.get().floatValue():0);
            u.putVec4(module.starShape.get().ordinal(),0,0,0);
        }
        starMatrix.set(RenderSystem.getModelViewMatrix()).mul(poses.last().pose());
        draw(STARS,stars,starIndices,starUniforms.currentBuffer(),"StarConfig",starMatrix);
        starUniforms.rotate();
    }
    private void rebuildStars() {
        if(stars!=null) { stars.close(); stars=null; }
        float[] field=StarField.generate(module.starAmount.get());
        try(var storage=ByteBufferBuilder.exactlySized(field.length/5*4*DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
            var builder=new BufferBuilder(storage,VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            for(int p=0;p<field.length;p+=5) {
                float x=field[p],y=field[p+1],z=field[p+2];
                // A stable tangent basis avoids the singularity at the poles.
                float tx=z,tz=-x,length=(float)Math.sqrt(tx*tx+tz*tz);
                if(length<.001f) { tx=1; tz=0; length=1; }
                tx/=length; tz/=length;
                float bx=y*tz,by=z*tx-x*tz,bz=-y*tx;
                float half=.16f*module.starSize.get().floatValue()*(.7f+field[p+4]*.5f);
                int color=0xFF000000|Math.round(field[p+3]*255)<<16|Math.round(field[p+4]*255)<<8;
                star(builder,x,y,z,tx,tz,bx,by,bz,-half,-half,0,0,color);
                star(builder,x,y,z,tx,tz,bx,by,bz,half,-half,1,0,color);
                star(builder,x,y,z,tx,tz,bx,by,bz,half,half,1,1,color);
                star(builder,x,y,z,tx,tz,bx,by,bz,-half,half,0,1,color);
            }
            try(MeshData mesh=builder.buildOrThrow()) {
                starIndices=mesh.drawState().indexCount();
                stars=RenderSystem.getDevice().createBuffer(()->"NexVisuals custom star field",GpuBuffer.USAGE_VERTEX,mesh.vertexBuffer());
            }
        }
        cachedCount=module.starAmount.get(); cachedSize=module.starSize.get();
    }
    private static void star(BufferBuilder b,float x,float y,float z,float tx,float tz,float bx,float by,float bz,float u,float v,float uvx,float uvy,int color) {
        b.addVertex(x*100+tx*u+bx*v,y*100+by*v,z*100+tz*u+bz*v).setUv(uvx,uvy).setColor(color);
    }
    private static void rgb(Std140Builder u,int color,float alpha) {
        u.putVec4(((color>>>16)&255)/255f,((color>>>8)&255)/255f,(color&255)/255f,(color>>>24)/255f*alpha);
    }
    private void draw(RenderPipeline pipeline,GpuBuffer vertices,int count,GpuBuffer settings,String name,Matrix4fc matrix) {
        var target=Minecraft.getInstance().getMainRenderTarget();
        var indices=RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        var indexBuffer=indices.getBuffer(count);
        var transforms=RenderSystem.getDynamicUniforms().writeTransform(matrix,white,zero,identity);
        try(RenderPass pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"NexVisuals sky",target.getColorTextureView(),OptionalInt.empty(),target.getDepthTextureView(),OptionalDouble.empty())) {
            pass.setPipeline(pipeline); RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms",transforms); pass.setUniform(name,settings);
            pass.setVertexBuffer(0,vertices); pass.setIndexBuffer(indexBuffer,indices.type()); pass.drawIndexed(0,0,count,1);
        }
    }
    @Override public void close() {
        if(dome!=null) { dome.close(); dome=null; }
        if(stars!=null) { stars.close(); stars=null; }
        if(skyUniforms!=null) { skyUniforms.close(); skyUniforms=null; }
        if(starUniforms!=null) { starUniforms.close(); starUniforms=null; }
        cachedCount=-1; cachedSize=-1;
    }
}
