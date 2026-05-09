package com.collaboration.exception;

/**
 * 网络异常类，用于表示网络通信层面的错误
 * 例如：连接断开、Socket 读写失败、连接超时等
 */
public class NetworkException extends RuntimeException {

    // ========== 构造方法 ==========

    /**
     * 无参构造方法
     * 实现步骤：
     * 1. 调用父类 RuntimeException 的无参构造方法
     */
    public NetworkException() {
        // TODO: 调用父类无参构造
        super();
    }

    /**
     * 带消息的构造方法
     * 实现步骤：
     * 1. 调用父类 RuntimeException 的构造方法，传入 message 参数
     */
    public NetworkException(String message) {
        // TODO: 调用父类构造方法，传入 message
        super(message);
    }

    /**
     * 带消息和原因的构造方法
     * 实现步骤：
     * 1. 调用父类 RuntimeException 的构造方法，传入 message 和 cause 参数
     */
    public NetworkException(String message, Throwable cause) {
        // TODO: 调用父类构造方法，传入 message 和 cause
        super(message, cause);
    }
}
