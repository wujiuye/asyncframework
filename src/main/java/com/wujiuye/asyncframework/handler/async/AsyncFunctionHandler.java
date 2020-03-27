package com.wujiuye.asyncframework.handler.async;

import org.objectweb.asm.ClassWriter;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

/**
 * 实现异步方法的Handler
 *
 * @author wujiuye 2019/11/24
 */
public interface AsyncFunctionHandler {

    boolean suppor(Class<?> interfaceClass, Method asyncMethod);

    /**
     * 实现异步方法
     *
     * @param classWriter          类改写器
     * @param interfaceClass       接口
     * @param asyncMethod          异步方法
     * @param proxyObjClass        接口的实现类
     * @param executorServiceClass 线程池的类型
     */
    void doOverrideAsyncFunc(ClassWriter classWriter, Class<?> interfaceClass, Method asyncMethod,
                             Class<?> proxyObjClass, Class<? extends ExecutorService> executorServiceClass);

}
