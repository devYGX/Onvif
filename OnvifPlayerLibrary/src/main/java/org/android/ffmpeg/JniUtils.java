package org.android.ffmpeg;

import java.lang.reflect.Method;

public class JniUtils {

    public static String getMethodSignature(Method method) {

        Class<?>[] parameterTypes = method.getParameterTypes();
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("(");
        if (parameterTypes.length > 0) {
            for (Class<?> parameterType : parameterTypes) {
                String classSignature = getClassSignature(parameterType);
                sBuilder.append(classSignature);
            }
        }
        sBuilder.append(")");
        Class<?> returnType = method.getReturnType();
        String returnTypeClassSignature = getClassSignature(returnType);
        sBuilder.append(returnTypeClassSignature);
        return sBuilder.toString();
    }

    public static String getNativeStaticRegisterFunctionName(Class clazz, Method method){
        return new StringBuilder("Java_")
                .append(clazz.getName().replace(".", "_").replace("$","_00024"))
                .append("_")
                .append(method.getName().replaceAll("_","_1"))
                .toString();
    }

    /**
     * 获取Class的签名
     *
     * @param clazz class对象
     * @return 签名
     */
    public static String getClassSignature(Class clazz) {
        if (clazz == null) return "";
        StringBuilder sBuilder = new StringBuilder();
        while ((clazz != null) && clazz.isArray()) {
            clazz = clazz.getComponentType();
            sBuilder.append("[");
        }
        if (clazz == null) return "";
        if (clazz == byte.class) return sBuilder.append("B").toString();
        if (clazz == char.class) return sBuilder.append("C").toString();
        if (clazz == short.class) return sBuilder.append("S").toString();
        if (clazz == int.class) return sBuilder.append("I").toString();
        if (clazz == long.class) return sBuilder.append("J").toString();
        if (clazz == float.class) return sBuilder.append("F").toString();
        if (clazz == double.class) return sBuilder.append("D").toString();
        if (clazz == boolean.class) return sBuilder.append("Z").toString();
        if (clazz == void.class) return sBuilder.append("V").toString();
        sBuilder.append("L");
        String classPath = clazz.getName().replace(".", "/");
        return sBuilder.append(classPath).append(";").toString();
    }
}
