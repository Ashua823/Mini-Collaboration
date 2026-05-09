package com.collaboration.exception;

/**
 * 全局异常处理器
 * 职责：统一处理不同类型的异常，返回标准化的错误响应字符串
 *
 * 错误响应格式：ERROR|异常类型|错误消息
 */
public class GlobalExceptionHandler {

    // ========== 静态方法 ==========

    /**
     * 处理异常并返回标准化的错误响应
     * @param e 捕获到的异常
     * @return 格式化的错误响应字符串
     *
     * 实现步骤：
     * 1. 判断异常类型（使用 instanceof）
     *    - BusinessException → 类型为 "BUSINESS_ERROR"
     *    - NetworkException → 类型为 "NETWORK_ERROR"
     *    - StorageException → 类型为 "STORAGE_ERROR"
     *    - 其他异常 → 类型为 "UNKNOWN_ERROR"
     * 2. 获取异常消息，如果消息为空则使用默认消息 "未知错误"
     * 3. 拼接并返回响应字符串，格式：ERROR|异常类型|错误消息
     */
    public static String handle(Throwable e) {
        // TODO: 实现步骤1-3
        String errorType;
        String message;

        switch (e) {
            case BusinessException be -> {
                errorType = "BUSINESS_ERROR";
                message = be.getMessage();
            }
            case NetworkException ne -> {
                errorType = "NETWORK_ERROR";
                message = ne.getMessage();
            }
            case StorageException se -> {
                errorType = "STORAGE_ERROR";
                message = se.getMessage();
            }
            case null, default -> {
                errorType = "UNKNOWN_ERROR";
                message = (e != null) ? e.getMessage() : null;
            }
        }

        if (message == null || message.isEmpty()) {
            message = "未知错误";
        }

        return "ERROR|" + errorType + "|" + message;
    }

    /**
     * 处理异常（带上下文信息）
     * @param e     捕获到的异常
     * @param context 上下文信息，如 "用户登录"、"文件上传" 等
     * @return 格式化的错误响应字符串
     *
     * 实现步骤：
     * 1. 调用 handle(Throwable e) 获取基础错误响应
     * 2. 在错误消息前拼接上下文信息，格式：ERROR|异常类型|[上下文]错误消息
     */
    public static String handle(Throwable e, String context) {
        // TODO: 实现步骤1-2
        //调用handle(Throwable e) 获取基础错误响应
        String baseResponse=handle(e);
        // 在错误消息前拼接上下文信息
        // 基础响应格式: ERROR|异常类型|错误消息
        // 目标格式: ERROR|异常类型|[上下文]错误消息
        //找到最后一个 | 的位置,将上下文插入到错误消息之前的位置
        int lastIndex=baseResponse.lastIndexOf("|");
        if(lastIndex!=-1){
            //ERROR异常类型
            String prefix = baseResponse.substring(0, lastIndex+1);
            String errorMessage = baseResponse.substring(lastIndex + 1);
            return prefix + "[" + context + "]" + errorMessage;
        }
        //如果解析失败,返回基础响应
        return baseResponse;
    }
}