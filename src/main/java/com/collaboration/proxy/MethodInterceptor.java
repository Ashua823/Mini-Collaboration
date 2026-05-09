package com.collaboration.proxy;

import java.lang.reflect.Method;

/**
 * 方法拦截器接口
 * 为什么拆成 before 和 after？
 * - 分离关注点：前置逻辑（权限校验）和后置逻辑（日志记录）职责不同
 * - 如果合并在一个方法里，拦截器内部还要手动调用 method.invoke()，
 *   那么 ServiceProxy 就会失去对方法调用的控制权
 * - 拆开后，ServiceProxy 统一控制方法调用时机，拦截器只负责自己的逻辑
 */
public interface MethodInterceptor {

    /**
     * 方法调用前的拦截
     * @param target 目标对象
     * @param method 被调用的方法
     * @param args   方法参数
     * 适用场景：权限校验、参数校验、事务开启
     *
     * 如果校验不通过，直接抛出异常即可阻止目标方法执行
     */
    default void before(Object target, Method method, Object[] args) throws Exception {
        // 默认空实现，子类可以选择性覆盖
    }

    /**
     * 方法调用后的拦截
     * @param target 目标对象
     * @param method 被调用的方法
     * @param args   方法参数
     * @param result 目标方法的返回值（如果方法抛异常则不会执行这里）
     * 适用场景：日志记录、结果处理、资源清理
     */
    default void after(Object target, Method method, Object[] args, Object result) throws Exception {
        // 默认空实现，子类可以选择性覆盖
    }
}