package dev.nexvisuals.client.post;

import dev.nexvisuals.core.animation.EnvironmentEnvelope;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;

/** Independent module metadata, but one shared scene copy/GPU pass and compatibility guard. */
final class WorldImageExtras {
    final WeatherLensModule weather;
    final UnderwaterEffectsModule water;
    final RetroDisplayModule retro;
    private final EnvironmentEnvelope rainBlend=new EnvironmentEnvelope(),waterBlend=new EnvironmentEnvelope();
    private Object level;
    private long inspectedTick=Long.MIN_VALUE;
    private double rainTarget;
    private boolean underwater;
    float rain,submerged;
    WorldImageExtras(WeatherLensModule weather,UnderwaterEffectsModule water,RetroDisplayModule retro) {
        this.weather=weather;this.water=water;this.retro=retro;
    }
    boolean requested() { return weather.enabled()&&weather.intensity.get()>0 || water.enabled()&&water.intensity.get()>0 || retro.enabled()&&retro.intensity.get()>0; }
    boolean active() { return rain>0 || submerged>0 || retro.enabled()&&retro.intensity.get()>0; }
    void update(Minecraft client,float partial) {
        if(!(weather.enabled()&&weather.intensity.get()>0) && !(water.enabled()&&water.intensity.get()>0)) { reset();return; }
        if(level!=client.level) { reset();level=client.level; }
        var camera=client.gameRenderer.getMainCamera();
        long tick=client.level.getGameTime();
        // Sky/biome precipitation checks once per tick, never per fragment or via a world scan.
        if(tick!=inspectedTick) {
            rainTarget=weather.enabled() && client.level.isRainingAt(camera.blockPosition())?client.level.getRainLevel(partial):0;
            underwater=camera.getFluidInCamera()==FogType.WATER;
            inspectedTick=tick;
        }
        double seconds=tick/20.0+partial/20.0;
        rain=weather.enabled()?(float)(rainBlend.update(underwater?0:rainTarget,seconds,weather.transition.get())*weather.intensity.get()):0;
        submerged=water.enabled()?(float)(waterBlend.update(underwater?1:0,seconds,water.transition.get())*water.intensity.get()):0;
        if(!weather.enabled()) rainBlend.reset();
        if(!water.enabled()) waterBlend.reset();
    }
    void reset() { rainBlend.reset();waterBlend.reset();rain=submerged=0;level=null;inspectedTick=Long.MIN_VALUE;rainTarget=0;underwater=false; }
}
