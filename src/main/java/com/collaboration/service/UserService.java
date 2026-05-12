package com.collaboration.service;



import com.collaboration.domain.Response;
import com.collaboration.domain.User;
import com.collaboration.storage.JsonStorage;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserService {

    private static JsonStorage jsonStorage = null;

    public UserService() {
        jsonStorage = new JsonStorage();
    }

    public JsonStorage getJsonStorage() {
        return jsonStorage;
    }

    //注册检查
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

    //登录检查
    public Response login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return Response.error("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Response.error("密码不能为空");
        }

        User user = jsonStorage.findByUsername(username);
        if (user == null) {
            return Response.error("用户名不存在");
        }

        if (!user.verifyPassword(password)) {
            return Response.error("密码错误");
        }

        // 只改内存状态，不保存到文件
        // 服务重启后所有人自动变为 offline，不需要持久化在线状态
        user.setStatus("online");
        // jsonStorage.save(user);  ← 删掉这行

        user.setPassword(null);
        user.setSalt(null);
        return Response.success(user);
    }

}
