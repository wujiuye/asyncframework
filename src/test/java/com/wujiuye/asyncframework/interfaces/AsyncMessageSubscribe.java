package com.wujiuye.asyncframework.interfaces;

import com.wujiuye.asyncframework.AsyncFunction;

/**
 * @author wujiuye
 * @version 1.0 on 2019/11/24 {描述：}
 */
public interface AsyncMessageSubscribe {

    @AsyncFunction
    void pullMessage(String queue);

}
