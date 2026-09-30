package dev.nexvisuals.client.sky;

import dev.nexvisuals.client.NexVisualsClient;
import dev.nexvisuals.client.integration.RenderCompatibility;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import dev.nexvisuals.core.visual.SkyMath;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FogType;

/** A procedural sky dome and star field, with independent settings and the existing config/preset codecs. */
public final class SkyboxModule extends VisualModule {
    public enum Stars { VANILLA, CUSTOM, DISABLED }
    public enum StarShape { PIXEL, DIAMOND, SOFT }
    public final BooleanSetting gradient=bool("gradient","Gradient sky",true);
    public final DoubleSetting gradientIntensity=number("gradient_intensity","Gradient intensity",1,0,1);
    public final DoubleSetting horizonHeight=number("horizon_height","Horizon height",0,-.2,.5);
    public final DoubleSetting horizonSoftness=number("horizon_softness","Horizon softness",.35,.05,.8);
    public final DoubleSetting brightness=number("brightness","Sky brightness",1,.4,1.6);
    public final DoubleSetting saturation=number("saturation","Sky saturation",1,0,1.6);
    public final ColorSetting tint=color("tint","Sky tint",0xFFFFFFFF);
    public final DoubleSetting dayNight=number("day_night","Day/night influence",1,0,1);
    public final ColorSetting daySky=color("day_sky","Day / custom sky color",0xFF5698DE);
    public final ColorSetting dayHorizon=color("day_horizon","Day horizon",0xFFB7D8EA);
    public final ColorSetting dayZenith=color("day_zenith","Day zenith",0xFF1E528E);
    public final ColorSetting sunsetSky=color("sunset_sky","Sunset / sunrise sky",0xFF835982);
    public final ColorSetting sunsetHorizon=color("sunset_horizon","Sunset / sunrise horizon",0xFFFFB472);
    public final ColorSetting sunsetZenith=color("sunset_zenith","Sunset / sunrise zenith",0xFF353A72);
    public final ColorSetting nightSky=color("night_sky","Night sky",0xFF0B142D);
    public final ColorSetting nightHorizon=color("night_horizon","Night horizon",0xFF26314C);
    public final ColorSetting nightZenith=color("night_zenith","Night zenith",0xFF030817);
    public final EnumSetting<Stars> stars=add(new EnumSetting<>("stars","Stars","Vanilla geometry, custom procedural field or disabled.",Stars.VANILLA,Stars.class));
    public final EnumSetting<StarShape> starShape=add(new EnumSetting<>("star_shape","Star type","Pixel squares, diamonds or soft round stars; custom mode only.",StarShape.SOFT,StarShape.class));
    public final IntSetting starAmount=add(new IntSetting("star_amount","Star amount","Custom stars, capped at 4000. Geometry rebuilds only when edited.",1800,0,4000));
    public final DoubleSetting starSize=add(new DoubleSetting("star_size","Star size","Custom star geometry only; vanilla mode retains Minecraft's star sizes.",1,.3,3));
    public final ColorSetting starColor=color("star_color","Star color",0xFFE2EBFF);
    public final DoubleSetting starBrightness=number("star_brightness","Star brightness",1,0,2);
    public final DoubleSetting starOpacity=number("star_opacity","Star opacity",.9,0,1);
    public final BooleanSetting twinkle=add(new BooleanSetting("twinkle","Twinkle","Custom stars pulse individually; vanilla stars use a shared subtle pulse.",true));
    public final DoubleSetting twinkleSpeed=number("twinkle_speed","Twinkle speed",1.4,.1,4);
    public final DoubleSetting twinkleIntensity=number("twinkle_intensity","Twinkle intensity",.35,0,1);
    public final DoubleSetting sunSize=number("sun_size","Sun size",1,.25,3);
    public final DoubleSetting sunOpacity=number("sun_opacity","Sun opacity",1,0,1);
    public final ColorSetting sunTint=color("sun_tint","Sun tint",0xFFFFFFFF);
    public final DoubleSetting moonSize=number("moon_size","Moon size",1,.25,3);
    public final DoubleSetting moonOpacity=number("moon_opacity","Moon opacity",1,0,1);
    public final ColorSetting moonTint=color("moon_tint","Moon tint",0xFFFFFFFF);
    public final BooleanSetting aurora=bool("aurora","Aurora ribbons",false);
    public final DoubleSetting auroraIntensity=number("aurora_intensity","Aurora intensity",.35,0,1);
    public final ColorSetting auroraColor=color("aurora_color","Aurora color",0xFF64E8B2);
    public final BooleanSetting nebula=bool("nebula","Nebula layer",false);
    public final DoubleSetting nebulaIntensity=number("nebula_intensity","Nebula intensity",.4,0,1);
    public final ColorSetting nebulaColor=color("nebula_color","Nebula color",0xFFA270DF);
    public final DoubleSetting atmosphereSpeed=number("atmosphere_speed","Atmosphere motion",.7,0,2);
    public final BooleanSetting shootingStars=bool("shooting_stars","Shooting stars",false);
    public final DoubleSetting meteorInterval=number("meteor_interval","Shooting star interval",18,6,40);
    public final BooleanSetting glow=bool("horizon_glow","Soft horizon glow",true);
    public final DoubleSetting glowIntensity=number("glow_intensity","Horizon glow intensity",.15,0,.8);
    public final ColorSetting glowColor=color("glow_color","Horizon glow color",0xFFFFB475);
    public final BooleanSetting fog=bool("fog","Atmospheric fog customization",false);
    public final ColorSetting fogColor=color("fog_color","Fog color",0xFF86A0C0);
    public final DoubleSetting fogBlend=number("fog_blend","Fog color intensity",.2,0,1);
    public final DoubleSetting fogDensity=add(new DoubleSetting("fog_density","Extra fog density","1 is vanilla; higher values add fog. Water/lava, blindness and darkness remain Minecraft-owned.",1,1,3));
    private final SkyboxRenderer renderer=new SkyboxRenderer(this);
    private Frame frame;
    private String failure="";
    public record Frame(int sky, int horizon, int zenith, double night, double sunset, float rain, float starVisibility, double seconds) { }

    public SkyboxModule() {
        super("skybox","Custom Skybox","Procedural gradient dome, custom stars, aurora, nebula and shooting stars. Keeps real sun/moon paths and phases. Pauses for active Iris packs.",Category.WORLD);
        group("General",gradient,gradientIntensity,horizonHeight,horizonSoftness,brightness,saturation,tint,dayNight);
        group("Day colors",daySky,dayHorizon,dayZenith);
        group("Sunset / sunrise colors",sunsetSky,sunsetHorizon,sunsetZenith);
        group("Night colors",nightSky,nightHorizon,nightZenith);
        group("Stars",stars,starShape,starAmount,starSize,starColor,starBrightness,starOpacity,twinkle,twinkleSpeed,twinkleIntensity);
        group("Sun & Moon",sunSize,sunOpacity,sunTint,moonSize,moonOpacity,moonTint);
        group("Atmosphere",aurora,auroraIntensity,auroraColor,nebula,nebulaIntensity,nebulaColor,atmosphereSpeed,shootingStars,meteorInterval,glow,glowIntensity,glowColor);
        group("Fog",fog,fogColor,fogBlend,fogDensity);
        preset("Vanilla+","A deeper gradient with familiar vanilla celestial bodies.");
        preset("Deep Night","Dense twinkling stars, dark blue zenith and a restrained green aurora.","stars","CUSTOM","star_amount",2600,"night_sky","#FF071426","night_zenith","#FF010510","aurora",true,"aurora_intensity",.3);
        preset("Purple Nebula","Cloud-like violet nebula, soft stars and a blue-purple night.","stars","CUSTOM","nebula",true,"nebula_intensity",.65,"night_sky","#FF241344","night_zenith","#FF0D092B","night_horizon","#FF453566","star_color","#FFF3CFFF","star_size",1.35);
        preset("Sunset","Warm horizons, a larger golden sun and a wide soft gradient.","day_horizon","#FFFFD7AB","sunset_horizon","#FFFF794B","sunset_sky","#FFAC597C","sun_size",1.25,"sun_tint","#FFFFD9AA","horizon_softness",.55,"glow_intensity",.38,"fog",true,"fog_color","#FFD2A9A4","fog_blend",.18);
        preset("Blood Moon","A large red moon with a crimson nebula and sharp stars.","stars","CUSTOM","star_shape","DIAMOND","moon_size",1.7,"moon_tint","#FFFF6464","night_sky","#FF2C1027","night_horizon","#FF662434","night_zenith","#FF080914","nebula",true,"nebula_color","#FF962642","nebula_intensity",.3);
        preset("Cyber","Cyan aurora, magenta nebula, diamonds and occasional meteors.","stars","CUSTOM","star_shape","DIAMOND","star_amount",2200,"star_color","#FF8DEEFF","aurora",true,"aurora_intensity",.65,"aurora_color","#FF3FE6DF","nebula",true,"nebula_intensity",.4,"nebula_color","#FFAD42C6","shooting_stars",true,"night_sky","#FF17152F");
        preset("Minimal","A calm gradient with a sparse pixel star field and no atmosphere motion.","stars","CUSTOM","star_shape","PIXEL","star_amount",600,"twinkle",false,"horizon_glow",false,"atmosphere_speed",0,"saturation",.75);
    }
    private BooleanSetting bool(String id,String name,boolean value) { return add(new BooleanSetting(id,name,"Cosmetic sky layer; toggles independently.",value)); }
    private DoubleSetting number(String id,String name,double value,double min,double max) { return add(new DoubleSetting(id,name,"Changes only sky appearance.",value,min,max)); }
    private ColorSetting color(String id,String name,int value) { return add(new ColorSetting(id,name,"Sky RGB color; celestial/atmospheric tint also supports alpha.",value)); }
    public boolean eligible(ClientLevel level,Camera camera) {
        if(!enabled() || !failure.isEmpty() || level.dimensionType().skybox()!=DimensionType.Skybox.OVERWORLD
                || camera.getFluidInCamera()!=FogType.NONE || !RenderCompatibility.blockReason().isEmpty()) return false;
        return !(camera.entity() instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS)||living.hasEffect(MobEffects.DARKNESS)));
    }
    public void extract(ClientLevel level,float partial,Camera camera,SkyRenderState state) {
        if(!eligible(level,camera)) { frame=null; return; }
        var weights=SkyMath.weights(state.sunAngle,dayNight.get());
        frame=new Frame(blend(daySky,sunsetSky,nightSky,weights),blend(dayHorizon,sunsetHorizon,nightHorizon,weights),
                blend(dayZenith,sunsetZenith,nightZenith,weights),weights.night(),weights.sunset(),state.rainBrightness,
                (float)(Math.clamp(state.starBrightness*2,0,1)*state.rainBrightness),((double)(level.getGameTime()%720_000)+partial)/20.0);
    }
    private int blend(ColorSetting day,ColorSetting sunset,ColorSetting night,SkyMath.Weights weights) {
        return SkyMath.grade(SkyMath.blend(day.get(),sunset.get(),night.get(),weights),brightness.get(),saturation.get(),tint.get());
    }
    public Frame frame() { return frame; }
    public boolean active() { return enabled() && failure.isEmpty() && frame!=null && RenderCompatibility.blockReason().isEmpty(); }
    public boolean renderDome() {
        if(!active()) return false;
        try { renderer.dome(frame); return true; } catch(RuntimeException exception) { fail(exception); return false; }
    }
    public boolean renderStars(com.mojang.blaze3d.vertex.PoseStack poses) {
        if(!active()) return false;
        if(stars.get()==Stars.DISABLED) return true;
        if(stars.get()!=Stars.CUSTOM) return false;
        try { renderer.stars(frame,poses); return true; } catch(RuntimeException exception) { fail(exception); return false; }
    }
    private void fail(RuntimeException exception) {
        failure="Paused: sky GPU pass failed. Check latest.log; toggle module to retry.";
        NexVisualsClient.LOGGER.error("Custom Skybox GPU pass failed; falling back to vanilla",exception);
        renderer.close(); frame=null;
    }
    @Override public String runtimeStatus() {
        if(!enabled()) return "Disabled; settings and presets remain saved.";
        String blocked=RenderCompatibility.blockReason();
        return !blocked.isEmpty()?blocked:!failure.isEmpty()?failure:"Ready in Overworld air; vanilla sky in other dimensions / vision effects.";
    }
    public void closeRenderer() { renderer.close(); frame=null; }
    @Override protected void onDisable() { closeRenderer(); failure=""; }
}
