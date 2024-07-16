package com.learn.flashsale.exception;

public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    protected String errCode;
    /**
     * 错误信息
     */
    protected String errDesc;

    public BizException() {
        super();
    }

    public BizException(BaseErrorInfoInterface errorInfoInterface) {
        super(errorInfoInterface.getErrCode());
        this.errCode = errorInfoInterface.getErrCode();
        this.errDesc = errorInfoInterface.getErrDesc();
    }

    public BizException(BaseErrorInfoInterface errorInfoInterface, Throwable cause) {
        super(errorInfoInterface.getErrCode(), cause);
        this.errCode = errorInfoInterface.getErrCode();
        this.errDesc = errorInfoInterface.getErrDesc();
    }

    public BizException(String errDesc) {
        super(errDesc);
        this.errDesc = errDesc;
    }

    public BizException(String errCode, String errDesc) {
        super(errCode);
        this.errCode = errCode;
        this.errDesc = errDesc;
    }

    public BizException(String errCode, String errDesc, Throwable cause) {
        super(errCode, cause);
        this.errCode = errCode;
        this.errDesc = errDesc;
    }


    public String getErrCode() {
        return errCode;
    }

    public void setErrCode(String errCode) {
        this.errCode = errCode;
    }

    public String getErrDesc() {
        return errDesc;
    }

    public void setErrDesc(String errDesc) {
        this.errDesc = errDesc;
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}