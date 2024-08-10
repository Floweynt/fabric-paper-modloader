package com.floweytf.papermixinloader.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.objectweb.asm.*;

/**
 * Budget @Redirect
 */
public class ReroutingCL extends ClassLoader {
    private record Entry(String owner, String name, String desc, boolean isStatic) {
    }

    private class ReroutingClassVisitor extends ClassVisitor {
        private ReroutingClassVisitor(ClassVisitor visitor) {
            super(Opcodes.ASM9, visitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
            return new ReroutingMethodVisitor(mv);
        }
    }

    private class ReroutingMethodVisitor extends MethodVisitor {
        private ReroutingMethodVisitor(MethodVisitor visitor) {
            super(Opcodes.ASM9, visitor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            final var newSig = entries.get(new Entry(owner, name, descriptor, opcode == Opcodes.INVOKESTATIC));

            if (newSig != null) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, newSig.owner, newSig.name, newSig.desc, isInterface);
            } else {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        }
    }

    private final Predicate<String> shouldTransform;
    private final Map<Entry, Entry> entries = new HashMap<>();

    public ReroutingCL(ClassLoader parent, Predicate<String> shouldTransform) {
        super(parent);
        this.shouldTransform = shouldTransform;
    }

    private String buildDesc(Class<?> returnType, List<Class<?>> args) {
        return Type.getMethodDescriptor(
            Type.getType(returnType),
            args.stream().map(Type::getType).toArray(Type[]::new)
        );
    }

    public ReroutingCL rerouteS(Class<?> old, String oldName, Class<?> newOwner, String newName, Class<?> returnType, Class<?>... argumentTypes) {
        final var desc = buildDesc(returnType, List.of(argumentTypes));

        entries.put(
            new Entry(Type.getInternalName(old), oldName, desc, true),
            new Entry(Type.getInternalName(newOwner), newName, desc, true)
        );

        return this;
    }

    public ReroutingCL rerouteI(Class<?> oldClass, String oldName, Class<?> newClass, String newName, Class<?> returnType, Class<?>... argumentTypes) {
        final var oldDesc = buildDesc(returnType, List.of(argumentTypes));
        final var newDesc = buildDesc(
            returnType,
            Stream.concat(Stream.of(oldClass), Arrays.stream(argumentTypes)).toList()
        );

        entries.put(
            new Entry(Type.getInternalName(oldClass), oldName, oldDesc, false),
            new Entry(Type.getInternalName(newClass), newName, newDesc, true)
        );
        return this;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!shouldTransform.test(name)) {
            return super.loadClass(name, resolve);
        }

        try (var input = super.getResourceAsStream(name.replaceAll("\\.", "/") + ".class")) {
            if (input == null) {
                throw new ClassNotFoundException("Failed to read class bytes from parent for " + name);
            }

            ClassReader reader = new ClassReader(input.readAllBytes());
            ClassWriter writer = new ClassWriter(reader, 0);
            reader.accept(new ReroutingClassVisitor(writer), 0);

            final var bytes = writer.toByteArray();

            final var clazz = defineClass(name, bytes, 0, bytes.length);

            if (resolve) {
                resolveClass(clazz);
            }

            return clazz;
        } catch (Throwable e) {
            throw new ClassNotFoundException("Failed to load class: " + name, e);
        }
    }
}
