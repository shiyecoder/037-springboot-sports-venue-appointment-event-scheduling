package com.grad.svs.common;

/**
 * 业务异常：由全局异常处理器转换为统一响应，避免把堆栈暴露给前端。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = 400;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
