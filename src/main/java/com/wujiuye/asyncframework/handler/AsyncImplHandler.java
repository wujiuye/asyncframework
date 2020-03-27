package com.wujiuye.asyncframework.handler;

import com.wujiuye.asyncframework.AsyncFunction;
import com.wujiuye.asyncframework.ByteCodeUtils;
import com.wujiuye.asyncframework.handler.async.AsyncFunctionHandler;
import com.wujiuye.asyncframework.handler.async.FutureAsyncFunctionHandler;
import com.wujiuye.asyncframework.handler.async.VoidAsyncFunctionHandler;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

/**
 * 对代理类再做一层AOP，对异步的支持
 *
 * @author wujiuye
 * @version 1.0 on 2019/11/24 {描述：}
 */
public class AsyncImplHandler implements ByteCodeHandler {

    private Class<?> tClass;
    private Class<? extends ExecutorService> executorServiceClass;
    private ClassWriter classWriter;

    private AsyncFunctionHandler[] asyncFunctionHandlers = new AsyncFunctionHandler[]{
            new VoidAsyncFunctionHandler(),
            new FutureAsyncFunctionHandler()
    };

    public AsyncImplHandler(Class<? extends ExecutorService> executorServiceClass, Class<?> tClass) {
        this.tClass = tClass;
        check();
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
        return ByteCodeUtils.getProxyClassName(tClass);
    }

    /**
     * 创建构造器，并且支持父类的带参构造器
     */
    private void extendsConstructor() {
        Constructor<?>[] constructors = tClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            Class<?>[] newParamTypes = new Class[paramTypes.length + 1];
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
            methodVisitor.visitMethodInsn(INVOKESPECIAL, tClass.getName().replace(".", "/"),
                    "<init>", ByteCodeUtils.getFuncDesc(null, paramTypes), false);

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
        Class<?>[] interfaces = tClass.getInterfaces();
        for (Class<?> in : interfaces) {
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
    protected void doOverrideAsyncFunc(Class<?> interfaceClass, Method asyncMethod) {
        for (AsyncFunctionHandler functionHandler : asyncFunctionHandlers) {
            if (functionHandler.suppor(interfaceClass, asyncMethod)) {
                functionHandler.doOverrideAsyncFunc(classWriter, interfaceClass, asyncMethod, tClass, executorServiceClass);
                return;
            }
        }
        throw new UnsupportedOperationException("temporary not suppor! interfaceClass:" + interfaceClass.getName() + ", function:" + asyncMethod.getName());
    }


    @Override
    public byte[] getByteCode() {
        // 类名、父类名、实现的接口名，以"/"替换'.'，注意，不是填类型签名
        this.classWriter.visit(Opcodes.V1_8, ACC_PUBLIC, getClassName(), null, tClass.getName().replace(".", "/"), null);
        // 添加字段executorService
        this.classWriter.visitField(ACC_PRIVATE, "executorService", Type.getDescriptor(executorServiceClass), null, null);
        extendsConstructor();
        overrideAsyncFunc();
        // end
        this.classWriter.visitEnd();
        return this.classWriter.toByteArray();
    }

}
