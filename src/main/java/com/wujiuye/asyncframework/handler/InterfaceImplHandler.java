package com.wujiuye.asyncframework.handler;

import com.wujiuye.asyncframework.ByteCodeUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.*;

/**
 * 实现接口并且是一个代理类
 *
 * @author wujiuye
 * @version 1.0 on 2019/11/23
 */
public class InterfaceImplHandler<T> implements ByteCodeHandler {

    private Class<T> interfaceClass;
    private ClassWriter classWriter;

    public InterfaceImplHandler(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
        this.check();
        this.classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    }

    private void check() {
        if (!this.interfaceClass.isInterface()) {
            throw new RuntimeException("class " + interfaceClass.getName() + " is not a interface!");
        }
    }

    @Override
    public String getClassName() {
        return interfaceClass.getName().replace(".", "/") + "ImplProxy";
    }

    /**
     * 初始化构造方法，带一个参数，即需要传入代理类
     * 参数类型就是接口类型
     */
    private void writeInitFunc() {
        MethodVisitor mv = classWriter.visitMethod(ACC_PUBLIC, "<init>", ByteCodeUtils.getFuncDesc(null, interfaceClass), null, null);
        mv.visitCode();
        // 解决java.lang.VerifyError: Constructor must call super() or this() before return
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V",false);
        // this.proxy = 参数1;'
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, getClassName(), "proxy", Type.getDescriptor(interfaceClass));
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    /**
     * 实现接口的所有方法
     */
    private void writeFuncImpl() {
        Method[] methods = interfaceClass.getDeclaredMethods();
        for (Method method : methods) {
            MethodVisitor mv = this.classWriter.visitMethod(ACC_PUBLIC, method.getName(), ByteCodeUtils.getFuncDesc(method.getReturnType(), method.getParameterTypes()), null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, getClassName(), "proxy", Type.getDescriptor(interfaceClass));
            for (int paramIndex = 1; paramIndex <= method.getParameterCount(); paramIndex++) {
                mv.visitVarInsn(ALOAD, paramIndex);
            }
            mv.visitMethodInsn(INVOKEINTERFACE, interfaceClass.getName().replace(".", "/"),
                    method.getName(), ByteCodeUtils.getFuncDesc(method.getReturnType(), method.getParameterTypes()),true);
            mv.visitInsn(RETURN);
            mv.visitMaxs(method.getParameterCount() + 2, method.getParameterCount() + 2);
            mv.visitEnd();
        }
    }

    @Override
    public byte[] getByteCode() {
        // 类名、父类名、实现的接口名，以"/"替换'.'，注意，不是填类型签名
        this.classWriter.visit(Opcodes.V1_8, ACC_PUBLIC, getClassName(), null, "java/lang/Object", new String[]{interfaceClass.getName().replace(".", "/")});
        // 添加字段proxy，访问标志为protected，因为第二层代理要用到
        this.classWriter.visitField(ACC_PROTECTED, "proxy", Type.getDescriptor(interfaceClass), null, null);
        // 添加构造方法
        this.writeInitFunc();
        // 实现接口的所有方法
        this.writeFuncImpl();
        // end
        this.classWriter.visitEnd();
        return this.classWriter.toByteArray();
    }

}
