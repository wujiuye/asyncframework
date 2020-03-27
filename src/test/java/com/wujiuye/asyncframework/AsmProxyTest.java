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

    private static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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

    public static class AsyncFutureProxy implements AsyncMessageSubscribe {

        private AsyncMessageSubscribe proxy;

        public AsyncFutureProxy(AsyncMessageSubscribe proxy) {
            this.proxy = proxy;
        }

        @Override
        public void pullMessage(String queue) {

        }

        @Override
        public AsyncResult<String> doAction(String s1, String s2) {
            Future<AsyncResult<String>> future = executorService.submit(new Callable<AsyncResult<String>>() {
                @Override
                public AsyncResult<String> call() throws Exception {
                    return proxy.doAction(s1, s2);
                }
            });
            // 再来一层代理，对外部屏蔽了线程阻塞等待
            return new AsyncResult<String>(null) {
                @Override
                public String get() throws InterruptedException, ExecutionException {
                    return future.get().get();
                }
            };
        }
    }

    @Test
    public void test2() {
        try {
            System.out.println(new AsyncFutureProxy(impl).doAction("jiuye", "wu").get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

}
