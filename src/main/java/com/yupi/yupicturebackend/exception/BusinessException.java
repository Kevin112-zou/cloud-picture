package com.yupi.yupicturebackend.exception;

import lombok.Getter;

/**
 * 自定义业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 自定义异常状态码，用于区分不同类型的异常
     */
    private final int code;

    public BusinessException(int code,String message){
        super(message);
        this.code = code;
    }
    public BusinessException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
    public BusinessException(ErrorCode errorCode,String message){
        super(message);
        this.code = errorCode.getCode();
    }
}
