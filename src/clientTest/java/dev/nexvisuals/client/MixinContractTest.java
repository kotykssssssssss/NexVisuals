package dev.nexvisuals.client;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Structural bytecode checks against the actual pinned game. This does not replace a live Mixin launch. */
class MixinContractTest {
    private ClassNode read(String internalName) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(internalName + ".class")) {
            assertNotNull(stream, internalName); ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0); return node;
        }
    }
    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values == null) return null;
        for (int i=0; i<annotation.values.size(); i+=2) if (annotation.values.get(i).equals(key)) return annotation.values.get(i+1);
        return null;
    }
    private static List<AnnotationNode> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        var result = new ArrayList<AnnotationNode>(); if (visible!=null) result.addAll(visible); if (invisible!=null) result.addAll(invisible); return result;
    }
    @Test void allMixinMethodsFieldsAndInvocationSitesExistInMinecraft12111() throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("nexvisuals.client.mixins.json")) {
            assertNotNull(stream);
            var json = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            int injectionCount = 0;
            for (var name : json.getAsJsonArray("client")) {
                ClassNode mixin = read(json.get("package").getAsString().replace('.', '/') + "/" + name.getAsString());
                AnnotationNode targetAnnotation = annotations(mixin.visibleAnnotations,mixin.invisibleAnnotations).stream()
                        .filter(a->a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElseThrow();
                Type targetType = (Type) ((List<?>) value(targetAnnotation,"value")).getFirst();
                ClassNode target = read(targetType.getInternalName());
                for (FieldNode field : mixin.fields) {
                    if (annotations(field.visibleAnnotations,field.invisibleAnnotations).stream().anyMatch(a->a.desc.endsWith("/Shadow;")))
                        assertTrue(target.fields.stream().anyMatch(f->f.name.equals(field.name)&&f.desc.equals(field.desc)), mixin.name+" shadow "+field.name);
                }
                for (MethodNode handler : mixin.methods) for (AnnotationNode injection : annotations(handler.visibleAnnotations,handler.invisibleAnnotations)) {
                    Object methodValue = value(injection,"method"), atValue = value(injection,"at");
                    if (!(methodValue instanceof List<?> methods) || atValue == null) continue;
                    injectionCount++;
                    for (Object methodObject : methods) {
                        String methodName = methodObject.toString();
                        List<MethodNode> candidates = target.methods.stream().filter(m -> methodName.equals(m.name) || methodName.equals(m.name+m.desc)).toList();
                        assertEquals(1, candidates.size(), mixin.name+" target "+methodName+" must be unambiguous");
                        MethodNode method = candidates.getFirst();
                        if (injection.desc.endsWith("/Inject;")) {
                            Type[] handlerArgs = Type.getArgumentTypes(handler.desc), targetArgs = Type.getArgumentTypes(method.desc);
                            assertEquals(targetArgs.length + 1, handlerArgs.length, handler.name+" captures all target args plus callback");
                            for (int i=0;i<targetArgs.length;i++) assertEquals(targetArgs[i],handlerArgs[i],handler.name+" arg "+i);
                        }
                        List<?> ats = atValue instanceof List<?> list ? list : List.of(atValue);
                        for (Object object : ats) {
                            AnnotationNode at = (AnnotationNode) object;
                            if (!"INVOKE".equals(value(at,"value"))) continue;
                            String invocation = (String) value(at,"target"); int count = 0;
                            for (AbstractInsnNode instruction : method.instructions) if (instruction instanceof MethodInsnNode call
                                    && invocation.equals("L"+call.owner+";"+call.name+call.desc)) count++;
                            Object ordinalValue = value(at,"ordinal"); int ordinal = ordinalValue instanceof Integer n ? n : -1;
                            assertTrue(count > Math.max(0,ordinal), handler.name+" missing invocation "+invocation);
                        }
                    }
                }
            }
            assertTrue(injectionCount >= 12, "Expected all declared adapters to be inspected");
        }
    }
}
