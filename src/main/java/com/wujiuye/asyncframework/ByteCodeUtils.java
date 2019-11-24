package com.wujiuye.asyncframework;

import jdk.internal.org.objectweb.asm.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author wujiuye
 * @version 1.0 on 2019/11/24 {描述：}
 */
public class ByteCodeUtils {

    /**
     * 获取方法签名
     *
     * @param returnType 方法返回值类型 null为方法无返回值
     * @param paramType  方法参数类型
     * @return
     */
    public static String getFuncDesc(Class returnType, Class... paramType) {
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
     * 将字节码转为class文件输出到类路径下
     *
     * @param byteCodeHandler 字节码处理器
     * @throws IOException
     */
    public static void savaToClasspath(ByteCodeHandler byteCodeHandler) throws IOException {
        String rootpath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        String className = byteCodeHandler.getClassName();
        File packageFile = new File(rootpath + className.substring(0, className.lastIndexOf("/")));
        if (!packageFile.exists() && !packageFile.mkdirs()) {
            throw new IOException("package create error or not found.");
        }
        File file = new File(rootpath + className + ".class");
        if ((!file.exists() || file.delete()) && file.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] byteCode = byteCodeHandler.getByteCode();
                fos.write(byteCode);
            }
        }
    }

}
