package com.xuecheng.base.exception;

public class XueChengPlusException extends RuntimeException{

    private String errMessage;

    public XueChengPlusException() {
        super();
    }

    public XueChengPlusException(String message) {
        super(message);
        this.errMessage=message;
    }

    public static void cast(CommonError commonError){
        throw  new XueChengPlusException(commonError.getErrMessage());
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }
}
