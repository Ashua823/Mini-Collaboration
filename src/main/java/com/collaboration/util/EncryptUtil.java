package main.java.com.collaboration.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class EncryptUtil {


    //	返回随机字符串
    public static String generateSalt(){
        return UUID.randomUUID().toString().replace("-", "");
    }

    //返回SHA-256加密后的十六进制字符串
    public static String sha256(String input){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不存在", e);
        }
    }
    //字节数组转十六进制字符串
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            // 使用 %02x 格式化为两位十六进制，自动补零
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }


}
