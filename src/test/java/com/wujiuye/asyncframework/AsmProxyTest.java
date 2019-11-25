package com.wujiuye.asyncframework;

import com.wujiuye.asyncframework.interfaces.AsyncMessageSubscribe;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author wujiuye
 * @version 1.0 on 2019/11/24 {描述：}
 */
public class AsmProxyTest {

    @Test
    public void testAutoProxyAsync() throws Exception {
        AsyncMessageSubscribe impl = (String queue) -> System.out.println(queue + "， current thread name:" + Thread.currentThread().getName());
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        AsyncMessageSubscribe proxy = AsmProxyFactory.getInterfaceImplSupporAsync(AsyncMessageSubscribe.class, impl, executorService);
        proxy.pullMessage("wujiuye");
        System.in.read();
    }

}
