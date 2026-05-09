package com.collaboration.proxy;



import com.collaboration.exception.BusinessException;

import java.lang.reflect.Method;

/**
 * 权限拦截器
 * 职责：在方法执行前校验用户权限，不通过则抛出异常阻止执行
 * 为什么拆成独立的拦截器？
 * - 单一职责原则：日志和权限是两个完全不同的关注点
 * - 可以单独开关：不需要权限校验时，不注册这个拦截器即可
 * - 可以灵活排序：注册顺序决定执行顺序，权限应该排在日志之前（先校验再记录）
 * 实现原理：
 * - 方法执行前，检查方法上是否有 @RequireAuth 注解
 * - 如果有，检查当前用户是否已登录、是否有权限
 * - 不通过则抛出 BusinessException，ServiceProxy 会捕获并阻止目标方法执行
 * 使用示例：
 *   @RequireAuth("ADMIN")
 *   public void deleteUser(String userId) { ... }
 */
public class AuthInterceptor implements MethodInterceptor {

    // ========== 属性定义 ==========

    /**
     * 当前登录用户的角色
     * 为什么用 static？
     * 这是一个简化实现。在真实项目中，用户信息从 Session/Token 中获取，
     * 每个线程的用户不同，应该用 ThreadLocal 存储。
     * 这里用 static 模拟全局单用户登录，方便测试。
     */
    private static String currentUserRole = null;

    // ========== 静态方法 ==========

    /**
     * 设置当前用户角色（模拟登录）
     * 调用时机：用户登录成功后调用
     * 为什么是静态方法？因为 currentUserRole 是 static，不需要实例就能访问
     */
    public static void setCurrentUserRole(String role) {
        currentUserRole = role;
    }

    /**
     * 清除当前用户角色（模拟登出）
     * 调用时机：用户登出时调用
     * 原因：如果不清理，下一个"登录"的用户可能绕过权限检查
     */
    public static void clearCurrentUserRole() {
        currentUserRole = null;
    }

    // ========== 拦截方法 ==========

    /**
     * 方法调用前的权限校验
     * 为什么只覆写 before 而不覆写 after？
     * 权限校验只需要在方法执行前做一次判断，不通过直接抛异常，
     * 方法根本不会执行，所以不需要 after 做任何清理工作。
     * 执行流程图：
     * 请求进入 → before() → 权限不通过抛异常（到此结束）
     *                    → 权限通过 → 目标方法执行 → after()（空实现）→ 返回
     *
     * @param target 目标对象（被代理的真实对象）
     * @param method 被调用的方法对象，通过它可以获取方法名、注解等信息
     * @param args   方法参数，这里用不到，但接口规定必须接收
     */
    @Override
    public void before(Object target, Method method, Object[] args) throws Exception {

        // 步骤1：检查方法上是否有 @RequireAuth 注解
        // isAnnotationPresent(Class) → 检查指定类型的注解是否存在于该方法上
        // 为什么不用 getAnnotation() != null？
        // isAnnotationPresent 语义更清晰，专门用于判断注解是否存在
        if (!method.isAnnotationPresent(RequireAuth.class)) {
            // 步骤2：没有注解 → 这个方法不需要权限，直接放行
            return;
        }

        // 步骤3：有注解 → 获取注解中声明的所需角色
        // getAnnotation(RequireAuth.class) → 获取方法上的 @RequireAuth 注解实例
        // .value() → 获取注解的 value 属性值
        // 返回值可能是 ""（默认值，表示只需登录）或 "ADMIN" 等（需要特定角色）
        String requiredRole = method.getAnnotation(RequireAuth.class).value();

        // 步骤4：判断需要哪种级别的权限
        if (requiredRole.isEmpty()) {
            // ===== 情况A：@RequireAuth 不带参数 =====
            // 表示该方法只需要用户登录即可调用，不限制具体角色
            // 示例：@RequireAuth 或 @RequireAuth("")
            if (currentUserRole == null) {
                // 没有登录 → 抛出业务异常
                // BusinessException 会被 ServiceProxy.invoke() 捕获并传播给调用方
                throw new BusinessException("请先登录");
            }
            // 已登录 → 放行（什么都不做，方法正常结束就是放行）
        } else {
            // ===== 情况B：@RequireAuth("ADMIN") 带具体角色 =====
            // 用户不仅需要登录，还必须是指定的角色
            if (currentUserRole == null) {
                // 先检查是否登录（如果不检查，下面的 equals 会空指针异常）
                throw new BusinessException("请先登录");
            }
            // 再检查角色是否匹配
            // 为什么用 requiredRole.equals(currentUserRole) 而不是反过来？
            // requiredRole 是注解值，一定不为 null；currentUserRole 已在上方判空
            if (!requiredRole.equals(currentUserRole)) {
                // 角色不匹配 → 抛出异常，告知用户需要什么角色
                throw new BusinessException("权限不足，需要角色: " + requiredRole);
            }
            // 角色匹配 → 放行
        }
    }

    // after 不需要做任何事
    // MethodInterceptor 接口中 after() 有 default 空实现，这里不需要覆写
}