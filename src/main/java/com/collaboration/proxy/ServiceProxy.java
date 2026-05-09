package com.collaboration.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务代理类
 * 职责：使用 JDK 动态代理创建服务对象的代理，组合多个拦截器实现链式调用
 * 为什么用动态代理？
 * - 可以在不修改原代码的情况下，给多个类统一添加功能（如日志、权限校验）
 * - 符合"开闭原则"：对扩展开放，对修改关闭
 * 缺点：
 * - 只能代理接口，不能代理普通类（JDK 动态代理的限制）
 * - 方法调用会经过反射，性能略低于直接调用（但通常可忽略）
 * 使用示例：
 *   UserService proxy = ServiceProxy.createProxy(userService, UserService.class);
 */
public class ServiceProxy {

    // ========== 属性定义 ==========

    /**
     * 全局拦截器列表
     * 为什么用 static？因为拦截器通常是全局配置，所有代理对象共享同一套拦截规则
     * 为什么用 List？因为拦截器需要按注册顺序依次执行（有序）
     */
    private static final List<MethodInterceptor> interceptors = new ArrayList<>();

    // ========== 静态方法 ==========

    /**
     * 注册拦截器
     * 适用场景：在应用启动时注册全局拦截器（如日志、权限校验）
     */
    public static void registerInterceptor(MethodInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    // ========== 核心方法 ==========

    /**
     * 创建代理对象
     * 为什么需要 interfaceType 参数？
     * JDK 动态代理要求必须传入接口类型，生成的代理对象实现这个接口。
     * 如果没有接口，需要使用 CGLIB（第三方库）来代理普通类。
     * 实现逻辑：
     * 1. 获取类加载器 → 用于加载动态生成的代理类字节码
     * 2. 指定接口 → 告诉 JVM 代理类需要实现哪些接口
     * 3. 定义 InvocationHandler → 代理对象每次调用方法时，都会转发到这个 handler
     * 4. Proxy.newProxyInstance → JDK 底层生成代理类的字节码并创建实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target, Class<T> interfaceType) {

        // 步骤1：获取目标对象的类加载器
        // 为什么要用 target 的类加载器？因为代理类需要和目标类在同一个类加载器环境中
        ClassLoader classLoader = target.getClass().getClassLoader();

        // 步骤2：创建接口数组
        // 为什么用数组？Proxy.newProxyInstance 的参数要求是 Class[]，支持代理多个接口
        Class<?>[] interfaces = new Class[]{interfaceType};

        // 步骤3：创建 InvocationHandler
        // 这是动态代理的核心——所有对代理对象的方法调用都会被拦截到这里
        InvocationHandler handler = new InvocationHandler() {

            /**
             * @param proxy  代理对象本身（一般不用，因为调用它会死循环）
             * @param method 被调用的方法对象（通过反射获取）
             * @param args   方法参数
             */
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
                try {
                    // === 前置拦截 ===
                    // 遍历所有拦截器，执行方法调用前的逻辑
                    // 为什么要在调用目标方法之前执行？
                    // 因为拦截器可能要做权限检查，不通过就直接抛异常，阻止目标方法执行
                    for (MethodInterceptor interceptor : interceptors) {
                        interceptor.before(target, method, args);
                    }

                    // === 调用目标方法 ===
                    // 反射调用：在 target 对象上执行 method 方法，传入 args 参数
                    // 返回值是目标方法的执行结果，需要保存下来
                    // method.invoke 为什么可能抛异常？
                    // - IllegalAccessException：没有权限访问该方法（通常是 private 方法）
                    // - InvocationTargetException：目标方法内部抛出了异常
                    Object result = method.invoke(target, args);

                    // === 后置拦截 ===
                    // 遍历所有拦截器，执行方法调用后的逻辑
                    // 为什么放在方法执行之后？
                    // 因为拦截器可能要做日志记录、结果处理等收尾工作
                    for (MethodInterceptor interceptor : interceptors) {
                        interceptor.after(target, method, args, result);
                    }

                    // 返回目标方法的执行结果
                    return result;

                } catch (InvocationTargetException e) {
                    // 目标方法内部抛出的异常被包装在 InvocationTargetException 中
                    // 需要取出真正的异常原因并抛出，否则调用方无法捕获到原始异常
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception) {
                        throw (Exception) cause;
                    }
                    throw new RuntimeException("方法调用失败: " + method.getName(), cause);
                } catch (IllegalAccessException e) {
                    // 无权访问方法时抛出（如访问了 private 方法）
                    throw new RuntimeException("无权访问方法: " + method.getName(), e);
                }
            }
        };

        // 步骤4：创建代理对象
        // Proxy.newProxyInstance 做了什么？
        // 1. 在内存中动态生成一个实现指定接口的 .class 字节码
        // 2. 用指定的 ClassLoader 加载这个字节码
        // 3. 创建这个类的实例，并将 handler 绑定到该实例上
        Object proxyInstance = Proxy.newProxyInstance(
                classLoader,      // 用哪个类加载器
                interfaces,       // 代理类要实现哪些接口
                handler           // 方法调用时由谁来处理
        );

        // 步骤5：返回代理对象
        // 因为 proxyInstance 实际类型是 $Proxy0（JVM动态生成的类），
        // 但它实现了 interfaceType 接口，所以可以强制转型为 T
        return (T) proxyInstance;
    }
}