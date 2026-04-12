package main.java.com.collaboration.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import main.java.com.collaboration.domain.User;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


public class JsonStorage {
    private static final String USERS_FILE_PATH = "data/users.json";

    //查找用户(根据用户名)
    public User findByUsername(String username) {
        // 1. 获取所有用户
        List<User> allUsers = findAll();
        // 2. stream + filter 查找
        // 3. 返回 findFirst().orElse(null)
         return allUsers.stream().filter(user->user.getUsername().equals(username))
                 .findFirst()
                 .orElse(null);
    }


    //保存单个用户
    public void save(User user) {
        List<User> users = findAll();
        users.add(user);
        saveAll(users);
    }

    //保存全部
    public void saveAll(List<User> users) {
        //确保目录存在
        ensureDirectoryExists();
        ObjectMapper mapper=new ObjectMapper();
        //启用格式化输出
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try(FileWriter fw=new FileWriter(USERS_FILE_PATH)){
            mapper.writeValue(fw,users);
        }catch (IOException e){
            throw new RuntimeException("保存用户数据失败",e);
        }
    }

    //查询所有用户
    public List<User> findAll(){
        //创建 ObjectMapper
        ObjectMapper mapper=new ObjectMapper();
        File file = new File(USERS_FILE_PATH);
        //文件不存在时返回空列表
        if(!file.exists()){
            return new ArrayList<>();
        }

        try(FileReader fr=new FileReader(file)){
            return mapper.readValue(fr, new TypeReference<List<User>>() {});
        } catch (IOException e) {
            //文件为空或者格式错误,返回空列表
            return new ArrayList<>();
        }

    }

    //检查目录是否存在
    private void ensureDirectoryExists() {
        File dir = new File("data");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    static void main() {
        JsonStorage storage = new JsonStorage();

        // 1. 测试空文件读取（首次运行）
        System.out.println("=== 测试 findAll（首次运行）===");
        List<User> users = storage.findAll();
        System.out.println("用户数量: " + users.size());
        System.out.println("用户列表: " + users);

        // 2. 测试保存单个用户
        System.out.println("\n=== 测试保存用户 ===");
        User user1 = new User();
        user1.setUserId("u001");
        user1.setUsername("张三");
        user1.setPassword("encrypted123");
        user1.setSalt("salt123");
        user1.setStatus("online");
        user1.setCreateTime(String.valueOf(LocalDateTime.now()));

        storage.save(user1);
        System.out.println("保存后立即读取: " + storage.findAll().size());
        System.out.println("用户1保存成功");

        // 3. 测试保存第二个用户
        User user2 = new User();
        user2.setUserId("u002");
        user2.setUsername("李四");
        user2.setPassword("encrypted456");
        user2.setSalt("salt456");
        user2.setStatus("offline");
        user2.setCreateTime(String.valueOf(LocalDateTime.now()));

        storage.save(user2);
        System.out.println("用户2保存成功");

        // 4. 测试 findAll 查看所有用户
        System.out.println("\n=== 测试 findAll（保存后）===");
        List<User> allUsers = storage.findAll();
        for (User u : allUsers) {
            System.out.println("用户名: " + u.getUsername() + ", ID: " + u.getUserId());
        }

        // 5. 测试 findByUsername
        System.out.println("\n=== 测试 findByUsername ===");
        User found = storage.findByUsername("张三");
        if (found != null) {
            System.out.println("找到用户: " + found.getUsername());
        } else {
            System.out.println("未找到用户");
        }

        User notFound = storage.findByUsername("王五");
        System.out.println("查找不存在的用户: " + (notFound == null ? "null" : "找到了"));




    }





}
