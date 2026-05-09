package com.collaboration.storage;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件存储操作类
 * 负责底层文件 IO 操作（字节流处理）
 */
public class FileStorage {

    private static final int CHUNK_SIZE = 1024 * 1024;  // 1MB
    // ========== 属性定义 ==========

    private final String storageRoot;  // 存储根目录（如 "data/files/"）

    // ========== 构造方法 ==========

    /**
     * 构造方法
     * @param storageRoot 存储根目录
     *
     * 实现步骤：
     * 1. 赋值 this.storageRoot = storageRoot
     * 2. 创建 File 对象指向 storageRoot
     * 3. 如果目录不存在，调用 mkdirs() 创建
     */
    public FileStorage(String storageRoot) {
        // TODO: 初始化存储根目录
        this.storageRoot=storageRoot;
        File file=new File(storageRoot);
        if(!file.exists()){
            boolean created=file.mkdirs();
            if(!created){
                System.out.println("创建存储目录失败:"+storageRoot);
            }
        }
    }

    // ========== 核心 IO 方法 ==========

    /**
     * 保存文件数据（支持追加模式）
     * @param relativePath 相对路径（如 "files/xxx.jpg"）
     * @param data 文件字节数据
     * @param append 是否追加模式（true=追加到末尾，false=覆盖）
     * @return 是否保存成功
     *
     * 实现步骤：
     * 1. 构建完整路径：Paths.get(storageRoot, relativePath).toString()
     * 2. 创建 File 对象，获取父目录
     * 3. 如果父目录不存在，调用 mkdirs() 创建
     * 4. 创建 FileOutputStream 对象，第二个参数传入 append
     * 5. 调用 write(data) 写入数据
     * 6. 调用 flush() 刷新缓冲区
     * 7. 关闭流（在 finally 中关闭）
     * 8. 捕获异常返回 false，成功返回 true
     */
    public boolean saveFileData(String relativePath, byte[] data, boolean append) {
        // TODO: 实现文件数据保存
        Path absolutePath = Paths.get(storageRoot, relativePath);
        File file=absolutePath.toFile();
        File parentDir=file.getParentFile();

        if(parentDir!=null && !parentDir.exists()){
            parentDir.mkdirs();
        }

        try (FileOutputStream fo=new FileOutputStream(file,append)){
            fo.write(data);
            fo.flush();
        } catch (IOException e) {
            return false;
        }


        return true;
    }

    /**
     * 从指定位置读取文件数据（用于断点续传）
     * @param relativePath 相对路径
     * @param offset 起始偏移量（字节位置）
     * @param length 读取长度（-1 表示读取到末尾）
     * @return 文件字节数据，失败返回 null
     *
     * 实现步骤：
     * 1. 构建完整路径
     * 2. 创建 RandomAccessFile 对象，模式为 "r"（只读）
     * 3. 调用 seek(offset) 跳到指定位置(用于设置文件指针的位置，允许你在文件中任意移动读写位置，实现文件的随机访问。)
     * 4. 计算实际读取长度：
     *    - 如果 length == -1，读取从 offset 到文件末尾
     *    - 否则读取 length 字节
     * 5. 创建 byte 数组
     * 6. 调用 read(byte[]) 读取数据
     * 7. 关闭流
     * 8. 返回读取的数据
     */
    public byte[] readFileData(String relativePath, long offset, int length) {
        // TODO: 实现文件数据读取
        Path absolutePath = Paths.get(storageRoot, relativePath);
        File file = absolutePath.toFile();
        if(!file.exists()){
            return null;
        }
        try (RandomAccessFile ra = new RandomAccessFile(file, "r")) {
           //跳到指定位置
            ra.seek(offset);
            //获取文件总长度
            long fileLength=ra.length();
            //获取剩余字节数
            long remaining=fileLength-offset;
            //实际读取长度
            int actualLength;
            if(length==-1){
                // 从 offset 读取到文件末尾
                actualLength=(int) remaining;
            }else {
                // 从 offset 开始读取 length 个字节
                // 但不能超过文件剩余字节数
                actualLength=(int) Math.min(length,remaining);
            }
            //实际长度小于等于0,返回空数组
            if(actualLength<=0){
                return null;
            }

            //读取数据
            byte[]buffer=new byte[actualLength];
            ra.read(buffer);
            return buffer;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 分块保存文件（用于模拟大文件分块上传）
     * @param relativePath 相对路径
     * @param chunkData 分块数据
     * @param chunkIndex 分块索引（从0开始）
     * @param totalChunks 总分块数
     * @return 是否保存成功
     *
     * 实现步骤：
     * 1. 计算当前分块的起始位置：chunkIndex * CHUNK_SIZE
     * 2. 使用 RandomAccessFile 以 "rw" 模式打开
     * 3. seek(起始位置) 跳到指定位置
     * 4. 写入分块数据
     * 5. 关闭流
     * 6. 返回成功或失败
     */
    public boolean saveChunk(String relativePath, byte[] chunkData, int chunkIndex, int totalChunks) {
        // TODO: 实现分块保存
        Path absolutePath = Paths.get(storageRoot, relativePath);
        File file = absolutePath.toFile();
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        long offset= (long) chunkIndex *CHUNK_SIZE;
        try (RandomAccessFile ra = new RandomAccessFile(file, "rw")) {
            ra.seek(offset);
            ra.write(chunkData);
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    // ========== 文件信息方法 ==========

    /**
     * 获取文件大小
     * @param relativePath 相对路径
     * @return 文件大小（字节），文件不存在返回 -1
     *
     * 实现步骤：
     * 1. 构建完整路径
     * 2. 创建 File 对象
     * 3. 如果文件存在，返回 length()
     * 4. 否则返回 -1
     */
    public long getFileSize(String relativePath) {
        // TODO: 返回文件大小
        Path absolutePath = Paths.get(storageRoot, relativePath).toAbsolutePath();
        File file=new File(absolutePath.toUri());
        if(file.exists()){
            return file.length();
        }

        return -1;
    }

    /**
     * 检查文件是否存在
     * @param relativePath 相对路径
     * @return 是否存在
     *
     * 实现步骤：
     * 1. 构建完整路径
     * 2. 创建 File 对象
     * 3. 返回 exists() 且 isFile()
     */
    public boolean fileExists(String relativePath) {
        // TODO: 检查文件是否存在
        Path absolutePath = Paths.get(storageRoot, relativePath).toAbsolutePath();
        File file=new File(absolutePath.toUri());

        return file.exists()&&file.isFile();


    }

    /**
     * 删除文件
     * @param relativePath 相对路径
     * @return 是否删除成功
     *
     * 实现步骤：
     * 1. 构建完整路径
     * 2. 创建 File 对象
     * 3. 如果文件存在，调用 delete()
     * 4. 返回删除结果
     */
    public boolean deleteFile(String relativePath) {
        // TODO: 删除文件
        Path absolutePath = Paths.get(storageRoot, relativePath).toAbsolutePath();
        File file=new File(absolutePath.toUri());
        if(file.exists()){
            return file.delete();
        }

        return false;
    }

    /**
     * 获取文件的最后修改时间
     * @param relativePath 相对路径
     * @return 最后修改时间戳（毫秒），失败返回 -1
     *
     * 实现步骤：
     * 1. 构建完整路径
     * 2. 创建 File 对象
     * 3. 如果文件存在，返回 lastModified()
     * 4. 否则返回 -1
     */
    public long getLastModified(String relativePath) {
        // TODO: 获取最后修改时间
        Path absolutePath = Paths.get(storageRoot, relativePath).toAbsolutePath();
        File file=new File(absolutePath.toUri());
        if (file.exists()){
            return file.lastModified();
        }
        return -1;
    }

    // ========== 辅助方法 ==========

    /**
     * 生成存储路径（基于文件ID和原始文件名）
     * @param fileId 文件唯一标识
     * @param originalName 原始文件名
     * @return 相对路径（如 "files/abc-123/photo.jpg"）
     *
     * 实现步骤：
     * 1. 从 originalName 提取文件扩展名（最后一个点之后的内容）
     * 2. 如果没有扩展名，使用空字符串
     * 3. 构建路径：storageRoot + fileId + "/" + fileId + 扩展名
     * 4. 返回路径
     *
     * 注意：用 fileId 作为文件夹名可以避免文件名冲突
     */
    public String generatePath(String fileId, String originalName) {
        // TODO: 生成存储路径
        //找到最后一个点的位置
        int lastDotIndex=originalName.lastIndexOf(".");
        //获取点之后的内容
        String extension =(lastDotIndex>=0) ? originalName.substring(lastDotIndex+1):"";
        return fileId+"/"+fileId+extension;
    }

    /**
     * 计算分块数量
     * @param fileSize 文件大小
     * @param chunkSize 每块大小（字节，如 1024 * 1024 = 1MB）
     * @return 分块数量
     *
     * 实现步骤：
     * 1. 如果 fileSize == 0，返回 1
     * 2. 返回 (fileSize + chunkSize - 1) / chunkSize（向上取整）
     */
    public int calculateTotalChunks(long fileSize, int chunkSize) {
        // TODO: 计算分块数量
        if(fileSize==0){
            return 1;
        }
       return Math.toIntExact((fileSize + chunkSize - 1) / chunkSize);
    }
}