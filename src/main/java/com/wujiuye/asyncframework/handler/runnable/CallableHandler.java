package com.wujiuye.asyncframework.handler.runnable;

import com.wujiuye.asyncframework.ByteCodeUtils;
import com.wujiuye.asyncframework.LogUtil;
import com.wujiuye.asyncframework.handler.ByteCodeHandler;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

/**
 * 实现Runnable
 *
 * @author wujiuye
 * @version 1.0 on 2020/03/27
 */
public class CallableHandler implements ByteCodeHandler {

    private Class<?> targetClass;
    private Method method;
    private ClassWriter classWriter;

    public CallableHandler(Class<?> targetClass, Method method) {
        this.targetClass = targetClass;
        this.method = method;
        this.classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    }

    @Override
    public String getClassName() {
        return targetClass.getName() + "_" + method.getName() + "Callable";
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
                "<init>", "()V", false);

        // this.target = var1;
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitFieldInsn(PUTFIELD,
                getClassName().replace(".", "/"),
                "target", Type.getDescriptor(initParams[0]));

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
     * 实现call方法
     *
     * @param initParams
     */
    private void writeCallFunc(Class<?>[] initParams) {
        String descriptor = "()Ljava/lang/Object;";
        String signature = descriptor;

        MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "call",
                // 方法描述
                descriptor,
                // 方法签名
                signature,
                // 异常
                null);

        methodVisitor.visitCode();

        // do printf log
        LogUtil.prinftLog(methodVisitor, "start run call......");

        methodVisitor.visitVarInsn(ALOAD, 0);
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
                    targetClass.getName().replace(".", "/"),
                    method.getName(),
                    Type.getMethodDescriptor(method),
                    true);
        } else {
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                    targetClass.getName().replace(".", "/"),
                    method.getName(),
                    Type.getMethodDescriptor(method),
                    false);
        }

        methodVisitor.visitInsn(ARETURN);
        methodVisitor.visitMaxs(3, 4);
        methodVisitor.visitEnd();
    }

    private String getClassSignature() {
        return "Ljava/lang/Object;Ljava/util/concurrent/Callable<Ljava/lang/Object;>;";
    }

    @Override
    public byte[] getByteCode() {
        classWriter.visit(Opcodes.V1_8, ACC_PUBLIC,
                getClassName().replace(".", "/"),
                getClassSignature(),
                "java/lang/Object",
                new String[]{Type.getInternalName(Callable.class)});

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
        this.writeCallFunc(initParams);
        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

}

