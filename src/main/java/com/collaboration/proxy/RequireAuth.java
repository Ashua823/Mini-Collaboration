package com.collaboration.proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限注解
 * 标记在方法上，表示该方法需要特定角色才能调用
 *
 * @Target(ElementType.METHOD)  → 只能用在方法上
 * @Retention(RetentionPolicy.RUNTIME)  → 运行时可通过反射读取
 *
 * 使用示例：
 *   @RequireAuth           → 只要登录就能访问
 *   @RequireAuth("ADMIN")  → 只有 ADMIN 角色能访问
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {

    /**
     * 所需角色，默认为空字符串表示只需登录
     */
    String value() default "";
}