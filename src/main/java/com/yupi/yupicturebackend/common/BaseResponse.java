package com.yupi.yupicturebackend.common;

import com.yupi.yupicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局响应封装类
 * @param <T>
 * Serializable 泛型标记接口，标记该对象可以被序列化
 */
@Data
public class BaseResponse<T> implements Serializable {

    private Integer code;

    private String message;

    private T data;

    public BaseResponse(Integer code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(Integer code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
