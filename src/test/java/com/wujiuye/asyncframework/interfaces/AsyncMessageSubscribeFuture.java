package com.wujiuye.asyncframework.interfaces;

import com.wujiuye.asyncframework.AsyncFunction;

/**
 * @author wujiuye
 * @version 1.0 on 2019/11/25 {描述：}
 */
public interface AsyncMessageSubscribeFuture {

    @AsyncFunction
    String asyncPull(String queue);

}
