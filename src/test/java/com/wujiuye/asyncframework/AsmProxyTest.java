package com.wujiuye.asyncframework;

import com.wujiuye.asyncframework.handler.async.AsyncResult;
import com.wujiuye.asyncframework.interfaces.AsyncMessageSubscribe;
import org.junit.Test;

import java.util.concurrent.*;

/**
 * @author wujiuye
 * @version 1.0 on 2019/11/24 {描述：}
 */
public class AsmProxyTest {

    /**
     * 用于异步执行方法
     */
    private static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * 接口的实现
     */
    private AsyncMessageSubscribe impl = new AsyncMessageSubscribe() {
        @Override
        public void pullMessage(String queue) {
            System.out.println(queue + "， current thread name:" + Thread.currentThread().getName());
        }

        @Override
        public AsyncResult<String> doAction(String s1, String s2) {
            System.out.println("s1==>" + s1 + ", s2==>" + s2);
            return new AsyncResult<>("hello wujiuye! current thread name:" + Thread.currentThread().getName());
        }
    };

    @Test
    public void testAutoProxyAsync() throws Exception {
        // 创建代理类
        // 结合spring使用其实很容易实现，就是使用Bean的后置处理器来调用这个方法，将返回的对象再交给spring去处理。因为spring已经有提供这样的功能了，
        // 所以我就不去做的，这个框架主要是给非spring环境下使用的。
        AsyncMessageSubscribe proxy = AsyncProxyFactory.getInterfaceImplSupporAsync(AsyncMessageSubscribe.class, impl, executorService);
        System.out.println("当前线程===>" + Thread.currentThread().getName());
        proxy.pullMessage("wujiuye");
        AsyncResult<String> asyncResult = proxy.doAction("sssss", "ddd");
        System.out.println(asyncResult.get());
        System.in.read();
    }

    /// ================  异步带返回值的实现原理 =======================

    public static class AsyncMessageSubscribe_doActionCallable implements Callable<AsyncResult<String>> {
        private AsyncMessageSubscribe target;
        private String param1;
        private String param2;

        public AsyncMessageSubscribe_doActionCallable(AsyncMessageSubscribe var1, String var2, String var3) {
            this.target = var1;
            this.param1 = var2;
            this.param2 = var3;
        }

        public AsyncResult<String> call() throws Exception {
            return this.target.doAction(this.param1, this.param2);
        }
    }

    @Test
    public void test2() throws ExecutionException, InterruptedException {
        AsyncMessageSubscribe_doActionCallable callable = new AsyncMessageSubscribe_doActionCallable(impl, "sada", "sdasad");
        Future result = executorService.submit(callable);
        AsyncResult<String> asyncResult = AsyncResult.newAsyncResultProxy(result);
        System.out.println(asyncResult.get());
    }


}
