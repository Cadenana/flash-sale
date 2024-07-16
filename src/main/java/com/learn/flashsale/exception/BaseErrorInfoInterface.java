package com.learn.flashsale.exception;

public interface BaseErrorInfoInterface {
    /**
     *  错误码
     */
    String getErrCode();

    /**
     * 错误描述
     */
    String getErrDesc();
}