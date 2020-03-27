package com.wujiuye.asyncframework;

import com.wujiuye.asyncframework.handler.AsyncImplHandler;
import com.wujiuye.asyncframework.handler.runnable.CallableHandler;
import com.wujiuye.asyncframework.handler.runnable.RunnableHandler;
import com.wujiuye.asyncframework.handler.InterfaceImplHandler;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.Callable;
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
public final class AsyncProxyFactory {

    private static Map<Class<?>, Class<?>> proxyMap = new ConcurrentHashMap<>();
    private static Map<Class<?>, Class<?>> asyncProxyMap = new ConcurrentHashMap<>();
    private static Map<String, Class<Runnable>> runnableMap = new ConcurrentHashMap<>();
    private static Map<String, Class<Callable<?>>> callableMap = new ConcurrentHashMap<>();

    private static final ByteCodeClassLoader CLASS_LOADER;
    private static final boolean DEBUG;

    static {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        CLASS_LOADER = AccessController.doPrivileged(new PrivilegedAction<ByteCodeClassLoader>() {
            @Override
            public ByteCodeClassLoader run() {
                return new ByteCodeClassLoader(loader);
            }
        });
        DEBUG = Boolean.getBoolean("debug");
    }

    /**
     * 实现的功能很简单，就是通过实现接口，为每个方法调用动态代理类
     *
     * @param tClass 接口类型
     * @param <T>    不限制任何接口
     * @return 返回接口的一个实现类
     */
    public static <T> Class<?> getInterfaceImpl(Class<T> tClass) throws Exception {
        if (proxyMap.containsKey(tClass)) {
            return proxyMap.get(tClass);
        }
        InterfaceImplHandler<T> interfaceImplHandler = new InterfaceImplHandler<>(tClass);
        CLASS_LOADER.add(interfaceImplHandler.getClassName(), interfaceImplHandler);
        if (DEBUG) {
            ByteCodeUtils.savaToClasspath(interfaceImplHandler);
        }
        Class<?> cla = CLASS_LOADER.loadClass(interfaceImplHandler.getClassName());
        proxyMap.put(tClass, cla);
        return cla;
    }

    /**
     * 获取支持@AsyncFunction的类
     *
     * @param tClass          含被声明异步执行的方法的接口
     * @param proxy           接口的实现类
     * @param executorService 执行任务的线程池
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> T getInterfaceImplSupporAsync(Class<T> tClass, T proxy, ExecutorService executorService) throws Exception {
        Class<?> proxyClass = getInterfaceImpl(tClass);
        synchronized (tClass) {
            if (!asyncProxyMap.containsKey(proxyClass)) {
                AsyncImplHandler asyncImplHandler = new AsyncImplHandler(ExecutorService.class, proxyClass);
                CLASS_LOADER.add(asyncImplHandler.getClassName(), asyncImplHandler);
                if (DEBUG) {
                    ByteCodeUtils.savaToClasspath(asyncImplHandler);
                }
                Class<?> cla = CLASS_LOADER.loadClass(asyncImplHandler.getClassName());
                asyncProxyMap.put(proxyClass, cla);
            }
        }
        // 根据参数类型获取相应的构造函数
        Constructor<?> constructor = asyncProxyMap.get(proxyClass).getConstructor(tClass, ExecutorService.class);
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
    public static Class<Runnable> getAsyncRunable(Class<?> targetClass, Method method) throws Exception {
        String key = targetClass.getName() + "$" + Type.getMethodDescriptor(method);
        if (runnableMap.containsKey(key)) {
            return runnableMap.get(key);
        }
        RunnableHandler runnableHandler = new RunnableHandler(targetClass, method);
        CLASS_LOADER.add(runnableHandler.getClassName(), runnableHandler);
        if (DEBUG) {
            ByteCodeUtils.savaToClasspath(runnableHandler);
        }
        Class cla = CLASS_LOADER.loadClass(runnableHandler.getClassName());
        runnableMap.put(key, cla);
        return cla;
    }

    /**
     * 获取Runnable
     *
     * @param targetClass runnable的run需要调用的方法的对象
     * @param method      runnable的run需要调用的方法
     * @return
     * @throws Exception
     */
    public static Class<Callable<?>> getAsyncCallable(Class<?> targetClass, Method method) throws Exception {
        String key = targetClass.getName() + "$" + Type.getMethodDescriptor(method);
        if (callableMap.containsKey(key)) {
            return callableMap.get(key);
        }
        CallableHandler callableHandler = new CallableHandler(targetClass, method);
        CLASS_LOADER.add(callableHandler.getClassName(), callableHandler);
        if (DEBUG) {
            ByteCodeUtils.savaToClasspath(callableHandler);
        }
        Class cla = CLASS_LOADER.loadClass(callableHandler.getClassName());
        callableMap.put(key, cla);
        return cla;
    }

}
