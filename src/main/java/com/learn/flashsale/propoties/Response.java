package com.learn.flashsale.propoties;

import com.learn.flashsale.exception.BaseErrorInfoInterface;
import com.learn.flashsale.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class Response implements Serializable {
    private Boolean success;
    private String errCode;
    private String errMessage;
    private Object data;

    public static Response success() {
        Response response = new Response();
        response.setSuccess(true);
        return response;
    }

    public static Response of(Object data) {
        Response response = new Response();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    public static Response error() {
        Response response = new Response();
        response.setSuccess(false);
        response.setErrCode(ErrorCode.UNKNOWN_ERROR.getErrCode());
        response.setErrMessage(ErrorCode.UNKNOWN_ERROR.getErrDesc());
        return response;
    }

    public static Response error(String errCode, String errMessage) {
        Response response = new Response();
        response.setSuccess(false);
        response.setErrCode(errCode);
        response.setErrMessage(errMessage);
        return response;
    }

    public static Response error(BaseErrorInfoInterface errorCode) {
        Response response = new Response();
        response.setSuccess(false);
        response.setErrCode(errorCode.getErrCode());
        response.setErrMessage(errorCode.getErrDesc());
        return response;
    }
}