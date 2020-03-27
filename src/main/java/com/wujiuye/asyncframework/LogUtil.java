package com.wujiuye.asyncframework;

import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class LogUtil {

    /**
     * 打印日记
     *
     * @param methodVisitor 方法访问器
     * @param logMsg        日记信息
     */
    public static void prinftLog(MethodVisitor methodVisitor, String logMsg) {
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        methodVisitor.visitLdcInsn(logMsg);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Object.class.getName().replace(".", "/"),
                "toString", "()Ljava/lang/String;", false);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

}
