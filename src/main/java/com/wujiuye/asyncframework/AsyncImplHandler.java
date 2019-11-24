package com.wujiuye.asyncframework;

import jdk.internal.org.objectweb.asm.Type;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

/**
 * 对代理类再做一层AOP，对异步的支持
 *
 * @author wujiuye
 * @version 1.0 on 2019/11/24 {描述：}
 */
public class AsyncImplHandler implements ByteCodeHandler {

    private Class tClass;
    private Class executorServiceClass;
    private ClassWriter classWriter;

    public AsyncImplHandler(Class executorServiceClass, Class tClass) {
        this.tClass = tClass;
        this.check();
        this.classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        this.executorServiceClass = executorServiceClass;
    }

    private void check() {
        if (this.tClass.isInterface()) {
            throw new RuntimeException("class " + tClass.getName() + " is a interface. not suppor interface!");
        }
    }

    @Override
    public String getClassName() {
        return tClass.getName().replace(".", "/") + "SupporAsync";
    }

    /**
     * 创建构造器，并且支持父类的带参构造器
     */
    private void extendsConstructor() {
        Constructor[] constructors = tClass.getConstructors();
        for (Constructor constructor : constructors) {
            Class[] paramTypes = constructor.getParameterTypes();
            Class[] newParamTypes = new Class[paramTypes.length + 1];
            for (int i = 0; i < paramTypes.length; i++) {
                newParamTypes[i] = paramTypes[i];
            }
            newParamTypes[newParamTypes.length - 1] = executorServiceClass;
            MethodVisitor methodVisitor = this.classWriter.visitMethod(ACC_PUBLIC, "<init>", ByteCodeUtils.getFuncDesc(null, newParamTypes), null, null);
            methodVisitor.visitCode();

            // 调用父类构造器
            methodVisitor.visitVarInsn(ALOAD, 0);
            for (int i = 0; i < paramTypes.length; i++) {
                methodVisitor.visitVarInsn(ALOAD, i + 1);
            }
            methodVisitor.visitMethodInsn(INVOKESPECIAL, tClass.getName().replace(".", "/"), "<init>", ByteCodeUtils.getFuncDesc(null, paramTypes));

            // 为executorService赋值
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, newParamTypes.length);
            methodVisitor.visitFieldInsn(PUTFIELD, getClassName(), "executorService", Type.getDescriptor(executorServiceClass));

            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(newParamTypes.length + 1, newParamTypes.length + 1);
            methodVisitor.visitEnd();
        }
    }

    /**
     * 覆写父类实现的所有接口的异步方法
     */
    private void overrideAsyncFunc() {
        Class[] interfaces = tClass.getInterfaces();
        for (Class in : interfaces) {
            Method[] methods = in.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(AsyncFunction.class)) {
                    doOverrideAsyncFunc(in, method);
                }
            }
        }
    }

    /**
     * 覆写方法支持异步
     *
     * @param asyncMethod @AsyncFunction方法
     */
    private void doOverrideAsyncFunc(Class interfaceClass, Method asyncMethod) {
        try {
            Class runableCla = AsmProxyFactory.getAsyncRunable(interfaceClass, asyncMethod);
            MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, asyncMethod.getName(), Type.getMethodDescriptor(asyncMethod), null, null);
            methodVisitor.visitCode();

            // new Runnable
            methodVisitor.visitTypeInsn(NEW, runableCla.getName().replace(".", "/"));
            methodVisitor.visitInsn(DUP);
            Class[] paramTypes = asyncMethod.getParameterTypes();
            Class[] newParamTypes = new Class[paramTypes.length + 1];

            newParamTypes[0] = interfaceClass;
            // 获取代理字段
            // super.proxy （要求tClass中必须有proxy字段，且子类可以访问）
            methodVisitor.visitVarInsn(ALOAD, 0);
            // 获取父类的字段，则第二个参数填父类名
            methodVisitor.visitFieldInsn(GETFIELD, tClass.getName().replace(".", "/"), "proxy", Type.getDescriptor(interfaceClass));

            int index = 1;
            for (; index < newParamTypes.length; index++) {
                newParamTypes[index] = paramTypes[index - 1];
                methodVisitor.visitVarInsn(ALOAD, index);
            }
            methodVisitor.visitMethodInsn(INVOKESPECIAL, runableCla.getName().replace(".", "/"), "<init>", ByteCodeUtils.getFuncDesc(null, newParamTypes));
            methodVisitor.visitVarInsn(ASTORE, index);

            // invoke submit runnable
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, getClassName(), "executorService", Type.getDescriptor(executorServiceClass));
            methodVisitor.visitVarInsn(ALOAD, index);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, executorServiceClass.getName().replace(".", "/"), "execute", "(Ljava/lang/Runnable;)V");

            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(index + 2, index);
            methodVisitor.visitEnd();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public byte[] getByteCode() {
        this.classWriter.visit(V1_8, ACC_PUBLIC, getClassName(), null, Type.getDescriptor(tClass), null);
        // 添加字段executorService
        this.classWriter.visitField(ACC_PRIVATE, "executorService", Type.getDescriptor(executorServiceClass), null, null);
        this.extendsConstructor();
        this.overrideAsyncFunc();
        // end
        this.classWriter.visitEnd();
        return this.classWriter.toByteArray();
    }

}
