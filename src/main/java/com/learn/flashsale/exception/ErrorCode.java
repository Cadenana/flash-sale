package com.learn.flashsale.exception;

public enum ErrorCode implements BaseErrorInfoInterface {
    NO_DATA("400", "暂无数据"),
    ILLEGAL_ARGUMENT("401", "参数异常"),
    USER_ABSENT("402", "用户不存在"),
    AUTH_ERROR("403", "权限不足"),
    OPERATION_FAILED("405", "操作失败"),
    ORDER_END_NUMBER_INVALID("406", "订单号核验异常"),
    ORDER_STATUS_INVALID("407", "订单状态不合法"),
    ORDER_STATUS_HAS_FINISHED("407", "已完成订单不可取消"),
    ID_MISS("408", "id不存在"),
    NO_SIGN_IN("409", "用户未登录"),
    LOGIN_ERROR("410", "登录出错"),
    USERNAME_EXIST("411", "用户名已存在"),
    TOKEN_PARSE_ERROR("412", "token解析错误，请重新登录"),
    LOGIN_EXPIRED("413", "登陆过期，请重新登录"),
    PARAM_ERROR("415", "参数异常"),
    PASSWORD_ERROR("416", "密码错误"),
    TEMPORARY_WITHDRAWAL_COED_ERROR("417", "临时提现码错误或已过期"),
    ORDER_NOT_EXIST("418", "订单不存在"),
    PAY_ERROR("514", "支付失败"),

    NULL_POINT_EXCEPTION("518", "参数为空"),
    REQUEST_FREQUENT("519", "访问频繁"),
    UNKNOWN_ERROR("1000", "服务器出错啦");

    private final String errCode;
    private final String errDesc;

    private ErrorCode(String errCode, String errDesc) {
        this.errCode = errCode;
        this.errDesc = errDesc;
    }

    public String getErrCode() {
        return errCode;
    }

    public String getErrDesc() {
        return errDesc;
    }
}