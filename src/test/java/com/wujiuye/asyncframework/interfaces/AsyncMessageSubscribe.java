package com.wujiuye.asyncframework.interfaces;

import com.wujiuye.asyncframework.AsyncFunction;
import com.wujiuye.asyncframework.handler.async.AsyncResult;

/**
 * @author wujiuye
 * @version 1.0 on 2019/11/24 {描述：}
 */
public interface AsyncMessageSubscribe {

    /**
     * 异步无返回值
     *
     * @param queue
     */
    @AsyncFunction
    void pullMessage(String queue);

    /**
     * 异步带返回值
     *
     * @param s1
     * @param s2
     * @return
     */
    @AsyncFunction
    AsyncResult<String> doAction(String s1, String s2);

}
