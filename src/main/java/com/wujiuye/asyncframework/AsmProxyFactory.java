package com.wujiuye.asyncframework;

import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 使用字节码技术(asm)动态实现接口
 * 注意点：
 * 1、就算方法没有返回值，也需要在方法后面加上一条return指令，否则就会一直报异常。
 *
 * @author wujiuye
 * @version 1.0 on 2019/11/23
 */
public final class AsmProxyFactory {

    private static Map<Class, Class> proxyMap = new ConcurrentHashMap<>();
    private static Map<Class, Class> asyncProxyMap = new ConcurrentHashMap<>();
    private static Map<String, Class<Runnable>> runnableMap = new ConcurrentHashMap<>();

    /**
     * 实现的功能很简单，就是通过实现接口，为每个方法调用动态代理类
     *
     * @param tClass 接口类型
     * @param <T>    不限制任何接口
     * @return 返回接口的一个实现类
     */
    public static <T> Class getInterfaceImplV2(Class<T> tClass) throws Exception {
        if (proxyMap.containsKey(tClass)) {
            return proxyMap.get(tClass);
        }
        InterfaceImplHandler<T> interfaceImplHandler = new InterfaceImplHandler<>(tClass);
        ByteCodeUtils.savaToClasspath(interfaceImplHandler);
        // 加载Class
        Class cla = Thread.currentThread().getContextClassLoader().loadClass(interfaceImplHandler.getClassName().replace("/", "."));
        proxyMap.put(tClass, cla);
        return cla;
    }

    /**
     * 获取支持@AsyncFunction的类
     *
     * @param tClass
     * @param proxy
     * @param executorService
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> T getInterfaceImplSupporAsync(Class<T> tClass, T proxy, ExecutorService executorService) throws Exception {
        Class proxyClass = getInterfaceImplV2(tClass);
        synchronized (AsmProxyFactory.class) {
            if (!asyncProxyMap.containsKey(proxyClass)) {
                AsyncImplHandler asyncImplHandler = new AsyncImplHandler(executorService.getClass(), proxyClass);
                ByteCodeUtils.savaToClasspath(asyncImplHandler);
                // 加载Class
                Class cla = Thread.currentThread().getContextClassLoader().loadClass(asyncImplHandler.getClassName().replace("/", "."));
                asyncProxyMap.put(proxyClass, cla);
            }
        }
        // 根据参数类型获取相应的构造函数
        Constructor constructor = asyncProxyMap.get(proxyClass).getConstructor(tClass, executorService.getClass());
        // 根据构造器创建实例，并传入代理类和线程池
        return (T) constructor.newInstance(proxy, executorService);
    }


    /**
     * 获取Runnable
     *
     * @param targetClass runnable的run需要调用的方法的对象
     * @param method      runnable的run需要调用的方法
     * @return
     * @throws Exception
     */
    public static Class<Runnable> getAsyncRunable(Class targetClass, Method method) throws Exception {
        String key = targetClass.getName() + "$" + Type.getMethodDescriptor(method);
        if (runnableMap.containsKey(key)) {
            return runnableMap.get(key);
        }
        AsyncRunnableHandler asyncRunnableHandler = new AsyncRunnableHandler(targetClass, method);
        ByteCodeUtils.savaToClasspath(asyncRunnableHandler);
        // 加载Class
        Class cla = Thread.currentThread().getContextClassLoader().loadClass(asyncRunnableHandler.getClassName().replace("/", "."));
        runnableMap.put(key, cla);
        return cla;
    }

}
