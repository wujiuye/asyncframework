package com.wujiuye.asyncframework.interfaces;

/**
 * @author wujiuye
 * @version 1.0 on 2019/11/24 {描述：}
 */
public interface AsyncMessageSubscribe {

    void pullMessage(String queue);

}
