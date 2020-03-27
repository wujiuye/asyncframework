package com.wujiuye.asyncframework.handler.async;

import org.objectweb.asm.ClassWriter;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

/**
 * 实现返回值类型为Future的异步方法的Handler
 *
 * @author wujiuye 2019/11/24
 */
public class FutureAsyncFunctionHandler implements AsyncFunctionHandler {

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
        throw new UnsupportedOperationException("temporary not suppor! interfaceClass:" + interfaceClass.getName() + ", function:" + asyncMethod.getName());
    }

}
