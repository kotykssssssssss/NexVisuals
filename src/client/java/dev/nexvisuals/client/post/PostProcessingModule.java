package dev.nexvisuals.client.post;

import dev.nexvisuals.client.NexVisualsClient;
import dev.nexvisuals.client.integration.RenderCompatibility;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** One optional world-image pass, before HUD/UI. Does not take ownership of vanilla entity post effects. */
public final class PostProcessingModule extends VisualModule {
    public final DoubleSetting intensity=number("intensity","Overall intensity",1,0,1);
    public final BooleanSetting grading=bool("grading","Color grading",true);
    public final DoubleSetting brightness=number("brightness","Brightness",1,.65,1.4);
    public final DoubleSetting contrast=number("contrast","Contrast",1,.65,1.5);
    public final DoubleSetting saturation=number("saturation","Saturation",1,0,1.6);
    public final DoubleSetting gamma=add(new DoubleSetting("gamma","Gamma","A bounded display curve; never changes world lighting. Black remains black.",1,.8,1.2));
    public final DoubleSetting temperature=number("temperature","Temperature",0,-1,1);
    public final DoubleSetting tint=number("tint","Green / magenta balance",0,-1,1);
    public final BooleanSetting vignette=bool("vignette","Vignette",true);
    public final DoubleSetting vignetteIntensity=number("vignette_intensity","Vignette intensity",.08,0,.65);
    public final DoubleSetting vignetteRadius=number("vignette_radius","Vignette radius",.55,.2,.9);
    public final DoubleSetting vignetteSoftness=number("vignette_softness","Vignette softness",.4,.1,.8);
    public final ColorSetting vignetteColor=color("vignette_color","Vignette color",0xFF090A12);
    public final BooleanSetting bloom=bool("bloom","Subtle glow",false);
    public final DoubleSetting bloomIntensity=number("bloom_intensity","Glow intensity",.12,0,.35);
    public final DoubleSetting bloomRadius=number("bloom_radius","Glow radius (pixels)",2.5,1,6);
    public final DoubleSetting bloomThreshold=number("bloom_threshold","Glow brightness threshold",.75,.6,.95);
    public final BooleanSetting chromatic=bool("chromatic","Chromatic aberration",false);
    public final DoubleSetting chromaticAmount=number("chromatic_amount","Channel separation (pixels)",.7,0,3);
    public final BooleanSetting grain=bool("grain","Film grain",false);
    public final DoubleSetting grainIntensity=number("grain_intensity","Grain intensity",.025,0,.07);
    public final BooleanSetting filter=bool("filter","Color filter",false);
    public final ColorSetting filterColor=color("filter_color","Filter color",0xFFD3DCFF);
    public final DoubleSetting filterIntensity=number("filter_intensity","Filter intensity",.2,0,.6);
    public final BooleanSetting nightTint=bool("night_tint","Night tint",false);
    public final ColorSetting nightColor=color("night_color","Night tint color",0xFF879FCC);
    public final DoubleSetting nightIntensity=number("night_intensity","Night tint intensity",.25,0,.6);
    public final BooleanSetting damage=bool("damage","Additional damage flash",false);
    public final ColorSetting damageColor=color("damage_color","Damage flash color",0xFFED625C);
    public final DoubleSetting damageIntensity=number("damage_intensity","Damage flash intensity",.12,0,.3);
    private final PostProcessingRenderer renderer=new PostProcessingRenderer(this);
    private String failure="";
    public PostProcessingModule() {
        super("post_processing","Lightweight Shaders","World-image color grading and small screen effects. One pass before HUD/UI; automatically paused for active Iris packs and low-vision effects.",Category.SHADERS);
        group("Color",intensity,grading,brightness,contrast,saturation,gamma,temperature,tint);
        group("Vignette",vignette,vignetteIntensity,vignetteRadius,vignetteSoftness,vignetteColor);
        group("Glow & optics",bloom,bloomIntensity,bloomRadius,bloomThreshold,chromatic,chromaticAmount);
        group("Film & filters",grain,grainIntensity,filter,filterColor,filterIntensity,nightTint,nightColor,nightIntensity);
        group("Damage flash",damage,damageColor,damageIntensity);
        preset("Vanilla+","Unchanged colors and a subtle vignette.");
        preset("Vibrant","Richer colors, gently stronger contrast and small bright-edge glow.","saturation",1.24,"contrast",1.08,"bloom",true,"bloom_intensity",.1,"vignette_intensity",.06);
        preset("Cinematic","Muted colors, warm balance, soft grain and a wider vignette.","saturation",.82,"contrast",1.13,"temperature",.25,"grain",true,"grain_intensity",.02,"vignette_intensity",.22);
        preset("Cold","Cool display balance and blue night tint.","temperature",-.65,"night_tint",true,"saturation",.9,"contrast",1.04);
        preset("Warm","Golden color balance and restrained glow.","temperature",.65,"bloom",true,"bloom_intensity",.08,"saturation",1.08);
        preset("Night","Cool muted grading with a stronger local night tint.","brightness",.88,"temperature",-.3,"night_tint",true,"night_intensity",.4,"vignette_intensity",.18);
        preset("Retro","Lower saturation, film texture and subtle channel separation.","saturation",.7,"contrast",1.2,"temperature",.2,"grain",true,"grain_intensity",.045,"chromatic",true,"chromatic_amount",.8,"vignette_intensity",.2);
    }
    private BooleanSetting bool(String id,String name,boolean value) { return add(new BooleanSetting(id,name,"Enable this world-image effect independently.",value)); }
    private DoubleSetting number(String id,String name,double value,double min,double max) { return add(new DoubleSetting(id,name,"Cosmetic image parameter. HUD and menu text are drawn afterwards.",value,min,max)); }
    private ColorSetting color(String id,String name,int value) { return add(new ColorSetting(id,name,"ARGB color; alpha multiplies this effect's intensity.",value)); }
    private boolean lowVision(Minecraft client) {
        return client.gameRenderer.getMainCamera().entity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS)||living.hasEffect(MobEffects.DARKNESS));
    }
    public void render(DeltaTracker delta) {
        if(!enabled()) return;
        var client=Minecraft.getInstance();
        if(client.level==null || client.player==null || intensity.get()==0 || lowVision(client) || !RenderCompatibility.blockReason().isEmpty()) {
            renderer.close(); return;
        }
        if(!failure.isEmpty()) return;
        try { renderer.render(client,delta); } catch(RuntimeException exception) {
            failure="Paused: post-processing GPU pass failed. See latest.log; toggle to retry.";
            NexVisualsClient.LOGGER.error("NexVisuals post-processing failed; disabling the optional pass until toggled",exception);
            renderer.close();
        }
    }
    @Override public String runtimeStatus() {
        if(!enabled()) return "Disabled; settings and presets remain saved.";
        String blocked=RenderCompatibility.blockReason();
        return !blocked.isEmpty()?blocked:!failure.isEmpty()?failure:"Ready; paused during Blindness/Darkness. HUD/UI is drawn after grading.";
    }
    public void closeRenderer() { renderer.close(); }
    @Override protected void onDisable() { closeRenderer(); failure=""; }
}
