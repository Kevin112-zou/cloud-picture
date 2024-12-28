package com.yupi.yupicturebackend.exception;

/**
 * 异常处理工具类
 */
public class ThrowUtils {
    /**
     * 条件成立则抛出运行时异常
     *
     * @param condition          需要判断的条件
     * @param runtimeException   需要抛出的运行时异常
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }
    /**
     * 如果条件满足，则抛出指定的业务异常。
     *
     * @param condition  需要判断的条件
     * @param errorCode  错误码，用于指示具体的错误类型
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
       throwIf(condition, new BusinessException(errorCode));
    }
    /**
     * 如果条件满足，则抛出指定的业务异常。
     *
     * @param condition  需要判断的条件
     * @param errorCode  错误码，用于指示具体的错误类型
     * @param message    错误消息，用于描述异常的具体情况
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode,message));
    }
}
