package com.wujiuye.asyncframework.handler.runnable;

import com.wujiuye.asyncframework.ByteCodeUtils;
import com.wujiuye.asyncframework.handler.ByteCodeHandler;
import org.objectweb.asm.Type;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.*;

/**
 * 实现Runnable
 *
 * @author wujiuye
 * @version 1.0 on 2019/11/24 {描述：}
 */
public class RunnableHandler implements ByteCodeHandler {

    private Class<?> targetClass;
    private Method method;
    private ClassWriter classWriter;

    public RunnableHandler(Class<?> targetClass, Method method) {
        this.targetClass = targetClass;
        this.method = method;
        this.classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    }

    @Override
    public String getClassName() {
        return targetClass.getName() + "_" + method.getName() + "Runnable";
    }

    /**
     * 添加构造器
     *
     * @param initParams
     */
    private void writeInitFunc(Class<?>[] initParams) {
        MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC,
                "<init>",
                ByteCodeUtils.getFuncDesc(null, initParams),
                null, null);
        methodVisitor.visitCode();
        // super()
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V", false);

        // this.target = var1;
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitFieldInsn(PUTFIELD,
                getClassName().replace(".", "/"),
                "target",
                Type.getDescriptor(initParams[0]));

        // this.paramX = varX+1;
        for (int i = 2; i <= initParams.length; i++) {
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, i);
            methodVisitor.visitFieldInsn(PUTFIELD,
                    getClassName().replace(".", "/"),
                    "param" + (i - 1),
                    Type.getDescriptor(initParams[i - 1]));
        }

        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitMaxs(initParams.length * 2, initParams.length + 1);
        methodVisitor.visitEnd();
    }

    /**
     * 实现run方法
     *
     * @param initParams
     */
    private void writeRunFunc(Class<?>[] initParams) {
        MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
        methodVisitor.visitCode();

        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitFieldInsn(GETFIELD,
                getClassName().replace(".", "/"),
                "target",
                Type.getDescriptor(targetClass));
        for (int i = 1; i < initParams.length; i++) {
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD,
                    getClassName().replace(".", "/"),
                    "param" + i,
                    Type.getDescriptor(initParams[i]));
        }
        if (targetClass.isInterface()) {
            methodVisitor.visitMethodInsn(INVOKEINTERFACE,
                    Type.getInternalName(targetClass),
                    method.getName(),
                    Type.getMethodDescriptor(method), true);
        } else {
            methodVisitor.visitMethodInsn(INVOKESPECIAL,
                    Type.getInternalName(targetClass),
                    method.getName(),
                    Type.getMethodDescriptor(method),
                    false);
        }

        methodVisitor.visitInsn(RETURN);
        methodVisitor.visitMaxs(initParams.length * 2, 1);
        methodVisitor.visitEnd();
    }

    @Override
    public byte[] getByteCode() {
        classWriter.visit(Opcodes.V1_8, ACC_PUBLIC,
                getClassName().replace(".","/"),
                null, 
                "java/lang/Object", 
                new String[]{Type.getInternalName(Runnable.class)});
        classWriter.visitField(ACC_PRIVATE, 
                "target", 
                Type.getDescriptor(targetClass), 
                null, null);
        Class<?>[] params = method.getParameterTypes();
        Class<?>[] initParams = new Class[params.length + 1];
        initParams[0] = targetClass;
        int index = 1;
        for (Class<?> param : params) {
            initParams[index] = param;
            classWriter.visitField(ACC_PRIVATE, 
                    "param" + index++, 
                    Type.getDescriptor(param), null, null);
        }
        this.writeInitFunc(initParams);
        this.writeRunFunc(initParams);
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

}
