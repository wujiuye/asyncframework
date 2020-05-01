package com.wujiuye.asyncframework;

import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author wujiuye
 * @version 1.0 on 2019/11/24 {描述：}
 */
public class ByteCodeUtils {

    /**
     * 获取方法描述
     *
     * @param returnType 方法返回值类型 null为方法无返回值
     * @param paramType  方法参数类型
     * @return
     */
    public static String getFuncDesc(Class<?> returnType, Class<?>... paramType) {
        String rTypeStr = returnType == null ? "V" : Type.getDescriptor(returnType);
        if (paramType.length == 0) {
            return "()" + rTypeStr;
        }
        StringBuilder funcDesc = new StringBuilder();
        funcDesc.append("(");
        for (Class cla : paramType) {
            funcDesc.append(Type.getDescriptor(cla));
        }
        funcDesc.append(")").append(rTypeStr);
        return funcDesc.toString();
    }

    /**
     * 获取方法签名
     *
     * @param method
     * @return
     */
    public static String getFunSignature(Method method) {
        try {
            Field field = method.getClass().getDeclaredField("signature");
            field.setAccessible(true);
            return field.get(method).toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将字节码转为class文件输出到类路径下，测试用
     *
     * @throws IOException
     */
    public static void savaToClasspath(String className, byte[] byteCode) {
        String rootpath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        File packageFile = new File(rootpath +
                className.substring(0, className.lastIndexOf("."))
                        .replace(".", "/"));
        if (!packageFile.exists() && !packageFile.mkdirs()) {
            return;
        }
        File file = new File(rootpath + className + ".class");
        try {
            if ((!file.exists() || file.delete()) && file.createNewFile()) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(byteCode);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
