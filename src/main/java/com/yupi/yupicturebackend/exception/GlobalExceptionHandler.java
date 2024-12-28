package com.yupi.yupicturebackend.exception;

import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class) // 捕获自定义的业务异常
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("businessException:", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }
    @ExceptionHandler(RuntimeException.class)
    public  BaseResponse<?> businessExceptionHandler(RuntimeException e) {
        log.error("businessException:", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统内部异常");
    }
}
