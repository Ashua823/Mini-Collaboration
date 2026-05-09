package com.collaboration;

import com.collaboration.proxy.RequireAuth;

/**
 * 测试服务接口
 * 为什么动态代理需要接口？
 * JDK 动态代理只能代理接口，被代理的对象必须实现至少一个接口
 */
public interface TestService {

    /** 公开方法，无需权限 */
    void publicMethod(String message);

    /** 需要登录才能调用 */
    @RequireAuth
    String loginRequired();

    /** 只有 ADMIN 角色才能调用 */
    @RequireAuth("ADMIN")
    void adminOnly();
}