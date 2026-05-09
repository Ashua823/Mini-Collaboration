package com.collaboration.exception;

/**
 * 业务异常类，用于表示业务逻辑层面的错误
 * 例如：用户名已存在、密码错误、无权限操作等
 */
public class BusinessException extends RuntimeException {

    // ========== 构造方法 ==========

    /**
     * 无参构造方法
     * 实现步骤：
     * 1. 调用父类 RuntimeException 的无参构造方法
     */
    public BusinessException() {
        // TODO: 调用父类无参构造
        super();
    }

    /**
     * 带消息的构造方法
     * 实现步骤：
     * 1. 调用父类 RuntimeException 的构造方法，传入 message 参数
     */
    public BusinessException(String message) {
        // TODO: 调用父类构造方法，传入 message
        super(message);
    }

    /**
     * 带消息和原因的构造方法
     * 实现步骤：
     * 1. 调用父类 RuntimeException 的构造方法，传入 message 和 cause 参数
     */
    public BusinessException(String message, Throwable cause) {
        // TODO: 调用父类构造方法，传入 message 和 cause
        super(message, cause);
    }
}