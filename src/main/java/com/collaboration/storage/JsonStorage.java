package com.collaboration.storage;


import com.collaboration.domain.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class JsonStorage {
    private static final String USERS_FILE_PATH = "data/users.json";

    //查找用户(根据用户名)
    public User findByUsername(String username) {
        // 1. 获取所有用户
        List<User> allUsers = findAll();
        // 2. stream + filter 查找
        // 3. 返回 findFirst().orElse(null)
        return allUsers.stream()
                .filter(user -> user != null && user.getUsername() != null && user.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }


    //保存单个用户
    public void save(User user) {
        List<User> users = findAll();
        // 查找是否已存在（按 userId 匹配，更新；按 username 匹配，替换）
        boolean found = false;
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            if (u != null && u.getUserId() != null && u.getUserId().equals(user.getUserId())) {
                users.set(i, user);  // 替换旧记录
                found = true;
                break;
            }
        }
        if (!found) {
            users.add(user);  // 新用户才追加
        }
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


}
