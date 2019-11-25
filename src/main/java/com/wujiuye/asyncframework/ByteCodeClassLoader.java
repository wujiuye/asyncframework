package com.wujiuye.asyncframework;

import com.wujiuye.asyncframework.handler.ByteCodeHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 需自定义类加载器
 *
 * @author wujiuye
 * @version 1.0 on 2019/11/25 {描述：}
 */
public class ByteCodeClassLoader extends ClassLoader {

    /**
     * 类名-> 字节码处理器 映射
     * 类名：以'.'分割包名的全类名
     */
    private final Map<String, ByteCodeHandler> classes = new HashMap<>();

    public ByteCodeClassLoader(final ClassLoader parentClassLoader) {
        super(parentClassLoader);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        ByteCodeHandler handler = classes.get(name);
        if (handler != null) {
            byte[] bytes = handler.getByteCode();
            return defineClass(name, bytes, 0, bytes.length);
        }
        return super.findClass(name);
    }

    public void add(final String name, final ByteCodeHandler handler) {
        classes.put(name.replace("/", "."), handler);
    }

    /**
     * 加载类
     *
     * @param name 全类名
     * @return
     * @throws ClassNotFoundException
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        name = name.replace("/", ".");
        return super.loadClass(name);
    }

    @Override
    protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

}
