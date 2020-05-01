package com.wujiuye.asyncframework;

import com.wujiuye.asyncframework.handler.ByteCodeHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 需自定义类加载器
 *
 * @author wujiuye
 * @version 1.0 on 2019/11/25 {描述：}
 */
public class ByteCodeClassLoader extends ClassLoader {

    private final Map<String, ByteCodeHandler> classes = new ConcurrentHashMap<>();

    public ByteCodeClassLoader(final ClassLoader parentClassLoader) {
        super(parentClassLoader);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        ByteCodeHandler handler = classes.get(name);
        if (handler != null) {
            byte[] bytes = handler.getByteCode();
            if (Boolean.getBoolean("async.debug")) {
                ByteCodeUtils.savaToClasspath(name, bytes);
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
        return super.findClass(name);
    }

    public void add(final String name, final ByteCodeHandler handler) {
        classes.putIfAbsent(name, handler);
    }

}
