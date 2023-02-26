package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    //处理xuechengplusexception异常
    @ResponseBody
    @ExceptionHandler(XueChengPlusException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse doxuechengplusexception(XueChengPlusException e){
        log.error("捕获异常信息{}",e.getMessage());

        String message = e.getMessage();
        return new RestErrorResponse(message);
    }

    //捕获不可知的异常
    //处理xuechengplusexception异常
    @ResponseBody
    @ExceptionHandler(XueChengPlusException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse doexception(Exception e){
        log.error("捕获异常信息{}",e.getMessage());
        return new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
    }
}
