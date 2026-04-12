package main.java.com.collaboration.service;

import main.java.com.collaboration.domain.Response;
import main.java.com.collaboration.domain.User;
import main.java.com.collaboration.storage.JsonStorage;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserService {

    private final JsonStorage jsonStorage;

    public UserService() {
        this.jsonStorage = new JsonStorage();
    }

    public JsonStorage getJsonStorage() {
        return jsonStorage;
    }

    public Response register(String username, String password) {
        // 1. 参数校验
        if (username == null || username.trim().isEmpty()) {
            return Response.error("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Response.error("密码不能为空");
        }

        // 2. 检查用户名是否已存在
        User existingUser = jsonStorage.findByUsername(username);
        if (existingUser != null) {
            return Response.error("用户名已存在");
        }

        // 3. 创建新用户
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setStatus("offline");
        user.setCreateTime(LocalDateTime.now().toString());

        // 4. 加密密码（注意：需要先设置盐和加密密码）
        user.encryptAndSetPassword(password);

        // 5. 保存用户
        jsonStorage.save(user);

        // 6. 返回成功
        user.setPassword(null);
        user.setSalt(null);
        return Response.success(user);

    }

    public Response login(String username, String password) {
        // 1. 参数校验
        if (username == null || username.trim().isEmpty()) {
            return Response.error("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Response.error("密码不能为空");
        }
        // 2. 查找用户
        User user = jsonStorage.findByUsername(username);
        if (user == null) {
            return Response.error("用户名不存在");
        }

        // 3. 密码验证
        if(user.verifyPassword(password)){
            return Response.error("密码错误");
        }

        // 4. 更新在线状态
        user.setStatus("online");
        //更新用户状态
        jsonStorage.save(user);

        // 5. 返回成功
        user.setPassword(null);
        user.setSalt(null);
        return Response.success(user);

    }

    public static void main(String[] args) {
        UserService userService = new UserService();

        // 测试注册
        System.out.println("=== 测试注册 ===");
        Response register1 = userService.register("testuser", "123456");
        System.out.println("注册结果: " + register1);

        // 重复注册测试
        Response register2 = userService.register("testuser", "123456");
        System.out.println("重复注册: " + register2);

        // 测试登录
        System.out.println("\n=== 测试登录 ===");
        Response login1 = userService.login("testuser", "123456");
        System.out.println("登录成功测试: " + login1);

        Response login2 = userService.login("testuser", "wrong");
        System.out.println("密码错误测试: " + login2);

        Response login3 = userService.login("nonexist", "123456");
        System.out.println("用户不存在测试: " + login3);
    }


}
