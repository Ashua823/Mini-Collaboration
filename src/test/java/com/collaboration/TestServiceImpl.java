package com.collaboration;

/**
 * 测试服务实现类
 */
public class TestServiceImpl implements TestService {

    @Override
    public void publicMethod(String message) {
        System.out.println("  → publicMethod 执行: " + message);
    }

    @Override
    public String loginRequired() {
        System.out.println("  → loginRequired 执行成功");
        return "success";
    }

    @Override
    public void adminOnly() {
        System.out.println("  → adminOnly 执行成功");
    }
}