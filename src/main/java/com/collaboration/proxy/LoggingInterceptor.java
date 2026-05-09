package com.collaboration.proxy;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 日志拦截器
 * 职责：在方法调用前后打印日志，实现 AOP 日志切面
 * 适用场景：
 * - 调试时追踪方法调用链路
 * - 记录关键操作的审计日志
 * - 性能分析（记录方法执行耗时）
 * 值得掌握的原因：
 * 这是 AOP（面向切面编程）最基础的应用，Spring AOP 的 @Around 就是这个原理。
 * 几乎所有后端框架都使用类似的拦截器链模式。
 */
public class LoggingInterceptor implements MethodInterceptor {

    /** 是否启用详细日志 */
    private final boolean verbose;

    /**
     * ThreadLocal 存储计时器
     * 什么是 ThreadLocal？
     * 你可以把它理解成每个线程私有的一个 HashMap。
     * 同样是 this.timerThreadLocal，线程A set 的值只有线程A能 get 到，
     * 线程B get 到的是线程B自己 set 的值，互不干扰。
     * 为什么用在这里？
     * 服务器用线程池处理请求，多个用户共享同一个 LoggingInterceptor 实例。
     * 如果不用 ThreadLocal，用户A的 startTime 会被用户B覆盖，计时就乱了。
     */
    private final ThreadLocal<LocalDateTime> timerThreadLocal = new ThreadLocal<>();

    // ========== 构造方法 ==========

    public LoggingInterceptor() {
        this.verbose = false;
    }

    public LoggingInterceptor(boolean verbose) {
        this.verbose = verbose;
    }

    // ========== 拦截方法 ==========

    @Override
    public void before(Object target, Method method, Object[] args) throws Exception {
        // 步骤1：获取当前时间戳
        // LocalDateTime.now() 返回当前日期时间，精度到纳秒
        LocalDateTime now = LocalDateTime.now();

        // 步骤2：拼接基础日志
        // target.getClass().getSimpleName() → 只取类名，不含包名，如 "UserService"
        // method.getName() → 获取方法名，如 "login"
        String log = String.format("[%s] 调用 %s.%s",
                now,
                target.getClass().getSimpleName(),
                method.getName());

        // 步骤3：如果 verbose=true，追加参数信息
        // Arrays.toString(args) 把参数数组转成可读字符串，如 "[admin, 123456]"
        // 如果 args 为 null，toString 会返回 "null"，这是可以接受的
        if (verbose) {
            log += String.format(" 参数: %s", Arrays.toString(args));
        }

        // 步骤4：把时间戳存入 ThreadLocal
        // set() 是把值存入当前线程的私有存储空间
        // 之后 after() 方法通过 get() 就能取出同一个时间戳
        timerThreadLocal.set(now);

        // 步骤5：打印日志
        System.out.println(log);
    }

    @Override
    public void after(Object target, Method method, Object[] args, Object result) throws Exception {
        // 步骤1：从 ThreadLocal 取出 startTime
        // 这里不要 new ThreadLocal()！要用成员变量 this.timerThreadLocal
        // get() 取出当前线程之前 set 进去的值
        LocalDateTime startTime = this.timerThreadLocal.get();

        // 步骤2：计算耗时
        // 如果 startTime 为 null（可能是 before 没执行就直接调了 after），给个默认值
        // Duration.between(起点, 终点) 计算两个时间点之间的差值
        // toMillis() 把差值转成毫秒数（long 类型）
        LocalDateTime endTime = LocalDateTime.now();
        long millis;
        if (startTime != null) {
            millis = Duration.between(startTime, endTime).toMillis();
        } else {
            millis = -1;  // -1 表示无法计算
        }

        // 步骤3：拼接日志
        String log = String.format("[%s] 完成 %s.%s - 耗时: %dms",
                endTime,
                target.getClass().getSimpleName(),
                method.getName(),
                millis);

        // 步骤4：如果 verbose=true，追加返回值信息
        if (verbose) {
            log += String.format(" 返回: %s", result);
        }

        // 步骤5：清理 ThreadLocal（防止内存泄漏）
        // 线程池的线程不会销毁，如果不 remove()，值会一直存在于内存中
        timerThreadLocal.remove();

        // 步骤6：打印日志
        System.out.println(log);
    }
}
