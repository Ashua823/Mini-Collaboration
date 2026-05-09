package com.collaboration.exception;

/**
 * 存储异常类，用于表示数据存储层面的错误
 * 例如：文件读写失败、JSON 解析错误、磁盘空间不足等
 */
public class StorageException extends RuntimeException {

    // ========== 构造方法 ==========

    /**
     * 无参构造方法
     * 实现步骤：
     * 1. 调用父类 RuntimeException 的无参构造方法
     */
    public StorageException() {
        // TODO: 调用父类无参构造
        super();
    }

    /**
     * 带消息的构造方法
     * 实现步骤：
     * 1. 调用父类 RuntimeException 的构造方法，传入 message 参数
     */
    public StorageException(String message) {
        // TODO: 调用父类构造方法，传入 message
        super(message);
    }

    /**
     * 带消息和原因的构造方法
     * 实现步骤：
     * 1. 调用父类 RuntimeException 的构造方法，传入 message 和 cause 参数
     */
    public StorageException(String message, Throwable cause) {
        // TODO: 调用父类构造方法，传入 message 和 cause
        super(message, cause);
    }
}