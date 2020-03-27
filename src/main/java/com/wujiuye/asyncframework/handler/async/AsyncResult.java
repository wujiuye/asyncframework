package com.wujiuye.asyncframework.handler.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 异步方法的返回参数
 *
 * @author wujiuye 2020/03/27
 */
public class AsyncResult<T> implements Future<T> {

    private T result;

    public AsyncResult(T result) {
        this.result = result;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return get();
    }

    /**
     * 由字节码调用
     *
     * @param future 提交到线程池执行返回的future
     * @param <T>
     * @return
     */
    public static <T> AsyncResult<T> newAsyncResultProxy(final Future<AsyncResult<T>> future) {
        return new AsyncResult<T>(null) {
            @Override
            public T get() throws InterruptedException, ExecutionException {
                AsyncResult<T> asyncResult = future.get();
                return asyncResult.get();
            }
        };
    }

    @Override
    public String toString() {
        return "AsyncResult{" +
                "result=" + result +
                '}';
    }

}
