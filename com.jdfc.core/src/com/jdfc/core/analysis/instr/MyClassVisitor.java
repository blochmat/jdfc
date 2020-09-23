package com.jdfc.core.analysis.instr;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MyClassVisitor extends ClassVisitor {

    final ClassNode classNode;
    final String className;
    final String jacocoMethodName = "$jacoco";

    public MyClassVisitor(ClassVisitor cv, ClassNode pClassNode) {
        super(Opcodes.ASM6, cv);
        classNode = pClassNode;
        className = classNode.name;
        System.out.println("DEBUG:" + className);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name,
                                     String desc, String signature, String[] exceptions) {
        MethodVisitor mv;
        mv = cv.visitMethod(access, name, desc, signature, exceptions);
        if (mv != null) {
            if(!isJacocoInstrumentation(name)) {
                MethodNode methodNode = getMethodNode(name);
                mv = new MyMethodVisitor(mv, className, name, desc, methodNode);
            }
        }
        return mv;
    }

    // TODO: Twice (see CFGCreatorVisitor)
    private MethodNode getMethodNode(String pName) {
        System.out.println("getMethodNode " + pName);
        for (MethodNode node : classNode.methods) {
            if (node.name.equals(pName)) {
                return node;
            }
        }
        return null;
    }

    private boolean isJacocoInstrumentation(String pString) {
        return pString.contains(jacocoMethodName);
    }
}
