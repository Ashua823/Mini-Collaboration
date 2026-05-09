package com.collaboration;

import com.collaboration.proxy.*;
import com.collaboration.exception.BusinessException;

/**
 * 拦截器集成测试
 * 职责：验证动态代理 + 拦截器链是否正常工作
 *
 * 测试场景：
 * 1. 无注解方法 → 日志正常，权限不拦截
 * 2. @RequireAuth 方法 → 未登录抛异常
 * 3. @RequireAuth 方法 → 已登录但角色不匹配抛异常
 * 4. @RequireAuth("ADMIN") → 角色匹配正常执行
 */
public class ProxyTest {

    public static void main(String[] args) {

        // ========== 1. 注册拦截器 ==========
        // 注意注册顺序：先权限校验，再日志记录
        // 如果权限不通过，日志拦截器的 after 也不会执行
        ServiceProxy.registerInterceptor(new AuthInterceptor());
        ServiceProxy.registerInterceptor(new LoggingInterceptor(true));  // true = 详细日志

        // ========== 2. 创建代理对象 ==========
        // 假设你的 UserService 有一个接口，这里用 TestService 演示
        TestService target = new TestServiceImpl();
        TestService proxy = ServiceProxy.createProxy(target, TestService.class);

        // ========== 3. 测试场景 ==========

        // 场景1：调用无注解的方法（不受权限限制）
        System.out.println("===== 场景1：公开方法 =====");
        proxy.publicMethod("hello");

        // 场景2：未登录调用需要权限的方法
        System.out.println("\n===== 场景2：未登录调用需登录的方法 =====");
        try {
            proxy.loginRequired();
        } catch (BusinessException e) {
            System.out.println("捕获到异常: " + e.getMessage());
        }

        // 场景3：登录但角色不匹配
        System.out.println("\n===== 场景3：角色不匹配 =====");
        AuthInterceptor.setCurrentUserRole("USER");
        try {
            proxy.adminOnly();
        } catch (BusinessException e) {
            System.out.println("捕获到异常: " + e.getMessage());
        }

        // 场景4：角色匹配正常执行
        System.out.println("\n===== 场景4：角色匹配 =====");
        AuthInterceptor.setCurrentUserRole("ADMIN");
        proxy.adminOnly();

        // 清理
        AuthInterceptor.clearCurrentUserRole();
        System.out.println("\n===== 测试完成 =====");
    }
}