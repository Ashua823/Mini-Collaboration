package main.java.com.collaboration.domain;


import main.java.com.collaboration.util.EncryptUtil;


public class User {
    //用户唯一标识
    private String userId;
    //用户名
    private String username;
    //密码
    private String password;
    //加密盐值
    private String salt;
    //在线离线状态
    private String status;
    //注册时间
    private String createTime;

    public User() {
    }

    public User(String userId, String username, String password, String salt, String status, String createTime) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.salt = salt;
        this.status = status;
        this.createTime = createTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", salt='" + salt + '\'' +
                ", status='" + status + '\'' +
                ", createTIme=" + createTime +
                '}';
    }

    public void encryptAndSetPassword(String rawPassword) {
        //生成盐值并存入salt
        setSalt(EncryptUtil.generateSalt());
        //原始密码和盐值拼接,存入密码
        rawPassword = rawPassword + getSalt();
        //调用 EncryptUtil.sha256(拼接后的字符串) 得到加密密码
        setPassword(EncryptUtil.sha256(rawPassword));
    }

    public boolean verifyPassword(String rawPassword) {
        //获取当前用户存储的盐值 this.salt
        String salt = getSalt();
        //将输入的密码和盐值拼接
        rawPassword = rawPassword + salt;
        //调用 EncryptUtil.sha256(拼接后的字符串) 得到加密结果
        String result = EncryptUtil.sha256(rawPassword);
        //比较这个结果和 this.password 是否相等
        return result.equals(getPassword());
    }



}