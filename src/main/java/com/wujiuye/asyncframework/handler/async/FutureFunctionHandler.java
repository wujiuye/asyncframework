package com.wujiuye.asyncframework.handler.async;

import com.wujiuye.asyncframework.AsyncProxyFactory;
import com.wujiuye.asyncframework.ByteCodeUtils;
import com.wujiuye.asyncframework.LogUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.objectweb.asm.Opcodes.*;

/**
 * 实现返回值类型为Future的异步方法的Handler
 *
 * @author wujiuye 2020/03/27
 */
public class FutureFunctionHandler implements AsyncFunctionHandler {

    @Override
    public boolean suppor(Class<?> interfaceClass, Method asyncMethod) {
        return asyncMethod.getReturnType() == AsyncResult.class;
    }

    /**
     * asyncMethod有返回值，且返回值类型为Future的处理
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
            Class<?> runableCla = AsyncProxyFactory.getAsyncCallable(interfaceClass, asyncMethod);
            MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, asyncMethod.getName(),
                    Type.getMethodDescriptor(asyncMethod), ByteCodeUtils.getFunSignature(asyncMethod), null);
            methodVisitor.visitCode();

            // new Callable
            methodVisitor.visitTypeInsn(NEW, runableCla.getName().replace(".", "/"));
            methodVisitor.visitInsn(DUP);
            Class<?>[] paramTypes = asyncMethod.getParameterTypes();
            Class<?>[] newParamTypes = new Class[paramTypes.length + 1];

            newParamTypes[0] = interfaceClass;
            // 获取代理字段
            // super.proxy （要求tClass中必须有proxy字段，且子类可以访问）
            methodVisitor.visitVarInsn(ALOAD, 0);
            // 获取父类的字段，则第二个参数填父类名
            methodVisitor.visitFieldInsn(GETFIELD, proxyObjClass.getName().replace(".", "/"), "proxy", Type.getDescriptor(interfaceClass));

            int index = 1;
            for (; index < newParamTypes.length; index++) {
                newParamTypes[index] = paramTypes[index - 1];
                methodVisitor.visitVarInsn(ALOAD, index);
            }
            methodVisitor.visitMethodInsn(INVOKESPECIAL, runableCla.getName().replace(".", "/"),
                    "<init>", ByteCodeUtils.getFuncDesc(null, newParamTypes), false);
            methodVisitor.visitVarInsn(ASTORE, index);

            // do printf log
            LogUtil.prinftLog(methodVisitor, "===============");

            // ===> 测试，非异步调用
//            methodVisitor.visitVarInsn(ALOAD, index);
//            methodVisitor.visitMethodInsn(INVOKEINTERFACE, Callable.class.getName().replace(".","/"),
//                    "call", ByteCodeUtils.getFuncDesc(AsyncResult.class), true);

            // invoke submit callable
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, ByteCodeUtils.getProxyClassName(proxyObjClass), "executorService", Type.getDescriptor(executorServiceClass));
            methodVisitor.visitVarInsn(ALOAD, index);
            if (!executorServiceClass.isInterface()) {
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, executorServiceClass.getName().replace(".", "/"),
                        "submit", ByteCodeUtils.getFuncDesc(Future.class, Callable.class), false);
            } else {
                methodVisitor.visitMethodInsn(INVOKEINTERFACE, executorServiceClass.getName().replace(".", "/"),
                        "submit", ByteCodeUtils.getFuncDesc(Future.class, Callable.class), true);
            }
            // 将返回值存到操作数栈
            methodVisitor.visitVarInsn(ASTORE, ++index);

            // 再来一层代理，对外部屏蔽线程阻塞等待
            methodVisitor.visitVarInsn(ALOAD, index);
            methodVisitor.visitMethodInsn(INVOKESTATIC, AsyncResult.class.getName().replace(".", "/"),
                    "newAsyncResultProxy", ByteCodeUtils.getFuncDesc(AsyncResult.class, Future.class),
                    false);

            methodVisitor.visitInsn(ARETURN);
            methodVisitor.visitMaxs(index + 2, index);
            methodVisitor.visitEnd();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
