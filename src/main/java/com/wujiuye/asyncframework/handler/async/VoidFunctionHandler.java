package com.wujiuye.asyncframework.handler.async;

import com.wujiuye.asyncframework.AsyncProxyFactory;
import com.wujiuye.asyncframework.ByteCodeUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

import static org.objectweb.asm.Opcodes.*;

/**
 * 实现无返回值异步方法的Handler
 *
 * @author wujiuye 2019/11/24
 */
public class VoidFunctionHandler implements AsyncFunctionHandler {

    @Override
    public boolean suppor(Class<?> interfaceClass, Method asyncMethod) {
        return asyncMethod.getReturnType() == void.class;
    }

    /**
     * asyncMethod没有返回值的处理
     *
     * @param classWriter          类改写器
     * @param interfaceClass       接口
     * @param asyncMethod          异步方法
     * @param proxyObjClass        接口的实现类
     * @param executorServiceClass 线程池的类型
     */
    @Override
    public void doOverrideAsyncFunc(ClassWriter classWriter, Class<?> interfaceClass, Method asyncMethod, Class<?> proxyObjClass, Class<? extends ExecutorService> executorServiceClass) {
        try {
            Class<?> runableCla = AsyncProxyFactory.getAsyncRunable(interfaceClass, asyncMethod);
            MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC,
                    asyncMethod.getName(),
                    Type.getMethodDescriptor(asyncMethod),
                    null, null);
            methodVisitor.visitCode();

            // new Runnable
            methodVisitor.visitTypeInsn(NEW, Type.getInternalName(runableCla));
            methodVisitor.visitInsn(DUP);
            Class<?>[] paramTypes = asyncMethod.getParameterTypes();
            Class<?>[] newParamTypes = new Class[paramTypes.length + 1];

            newParamTypes[0] = interfaceClass;
            // 获取代理字段
            // super.proxy （要求tClass中必须有proxy字段，且子类可以访问）
            methodVisitor.visitVarInsn(ALOAD, 0);
            // 获取父类的字段，则第二个参数填父类名
            methodVisitor.visitFieldInsn(GETFIELD,
                    Type.getInternalName(proxyObjClass),
                    "proxy", Type.getDescriptor(interfaceClass));

            int index = 1;
            for (; index < newParamTypes.length; index++) {
                newParamTypes[index] = paramTypes[index - 1];
                methodVisitor.visitVarInsn(ALOAD, index);
            }
            methodVisitor.visitMethodInsn(INVOKESPECIAL,
                    Type.getInternalName(runableCla),
                    "<init>",
                    ByteCodeUtils.getFuncDesc(null, newParamTypes),
                    false);
            methodVisitor.visitVarInsn(ASTORE, index);

            // invoke submit runnable
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD,
                    Type.getInternalName(proxyObjClass) + "SupporAsync",
                    "executorService",
                    Type.getDescriptor(executorServiceClass));
            methodVisitor.visitVarInsn(ALOAD, index);
            if (!executorServiceClass.isInterface()) {
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                        Type.getInternalName(executorServiceClass),
                        "execute",
                        "(Ljava/lang/Runnable;)V", false);
            } else {
                methodVisitor.visitMethodInsn(INVOKEINTERFACE,
                        Type.getInternalName(executorServiceClass),
                        "execute",
                        "(Ljava/lang/Runnable;)V", true);
            }
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(index + 2, index);
            methodVisitor.visitEnd();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
