package text;

import main.java.com.collaboration.service.UserService;

public class TestRegister {
    static void main() {
        UserService userService = new UserService();

        // 注册测试用户
        System.out.println("注册 user1:");
        System.out.println(userService.register("user1", "123456"));

        System.out.println("\n注册 user2:");
        System.out.println(userService.register("user2", "123456"));

        System.out.println("\n注册重复用户名:");
        System.out.println(userService.register("user1", "123456"));
    }
}
