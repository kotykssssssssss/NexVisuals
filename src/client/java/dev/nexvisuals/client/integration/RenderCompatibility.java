package dev.nexvisuals.client.integration;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.fabricmc.loader.api.FabricLoader;

/** Optional public Iris API, resolved once. No dependency or reflection search in rendering paths. */
public final class RenderCompatibility {
    private static final boolean IRIS=FabricLoader.getInstance().isModLoaded("iris");
    private static boolean resolved;
    private static MethodHandle packInUse;
    private static boolean failed;
    private RenderCompatibility() { }
    public static String blockReason() {
        if(!IRIS) return "";
        if(!resolved) {
            resolved=true;
            try {
                Class<?> api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object instance=api.getMethod("getInstance").invoke(null);
                packInUse=MethodHandles.publicLookup().unreflect(api.getMethod("isShaderPackInUse"))
                        .bindTo(instance).asType(MethodType.methodType(boolean.class));
            } catch(ReflectiveOperationException | LinkageError exception) { failed=true; }
        }
        if(!failed) try {
            if((boolean)packInUse.invokeExact()) return "Paused: active Iris shader pack owns this rendering pass.";
            return "";
        } catch(Throwable exception) { failed=true; }
        return "Paused: installed Iris API could not be checked safely.";
    }
}
