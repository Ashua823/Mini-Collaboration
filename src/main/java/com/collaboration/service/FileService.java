package com.collaboration.service;


import com.collaboration.domain.FileInfo;
import com.collaboration.storage.FileStorage;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 文件业务逻辑层
 * 负责文件的上传、下载、元数据管理
 */
public class FileService {

    // ========== 常量定义 ==========

    private static final int CHUNK_SIZE = 1024 * 1024;  // 1MB 每块（模拟断点续传）

    // ========== 属性定义 ==========

    private final ConcurrentHashMap<String, FileInfo> fileInfoMap;  // 文件ID → 文件元信息
    private final FileStorage fileStorage;                          // 底层文件存储
    private final String metadataPath;                              // 元数据文件路径
    private final ObjectMapper objectMapper;                        // JSON 解析器
    private final ReentrantReadWriteLock lock;                      // 读写锁

    // ========== 构造方法 ==========

    /**
     * 构造方法
     * @param storageDir 文件存储根目录（如 "data/files/"）
     * @param metadataPath 元数据文件路径（如 "data/files-metadata.json"）
     *
     * 实现步骤：
     * 1. 初始化 fileInfoMap = new ConcurrentHashMap<>()
     * 2. 初始化 fileStorage = new FileStorage(storageDir)
     * 3. 初始化 objectMapper = new ObjectMapper()
     * 4. 配置 objectMapper 忽略未知字段（FAIL_ON_UNKNOWN_PROPERTIES false）
     * 5. 初始化 lock = new ReentrantReadWriteLock()
     * 6. 调用 loadMetadata() 加载已有元数据
     */
    public FileService(String storageDir, String metadataPath) {
        // TODO: 初始化所有属性
        fileInfoMap=new ConcurrentHashMap<>();
        fileStorage=new FileStorage(storageDir);
        objectMapper=new ObjectMapper();
        lock=new ReentrantReadWriteLock();
        this.metadataPath=metadataPath;

        // TODO: 加载元数据
        loadMetadata();
    }

    // ========== 元数据持久化 ==========

    /**
     * 从文件加载元数据
     *
     * 实现步骤：
     * 1. 创建 File file = new File(metadataPath)
     * 2. 如果文件不存在，直接返回
     * 3. 使用 objectMapper 读取 List<FileInfo>
     * 4. 获取写锁 lock.writeLock().lock()
     * 5. 清空 fileInfoMap
     * 6. 将读取的数据逐个放入 fileInfoMap（key 为 fileId）
     * 7. 释放写锁
     * 8. 捕获异常打印错误日志
     */
    private void loadMetadata() {
        // TODO: 从 JSON 文件加载元数据
        File file=new File(metadataPath);
        if(!file.exists()){
            return;
        }

        lock.writeLock().lock();
        try {
            List<FileInfo>fileInfoList=objectMapper.readValue(file,new TypeReference<List<FileInfo>>(){});
            fileInfoMap.clear();
            for (FileInfo fileInfo : fileInfoList) {
                fileInfoMap.put(fileInfo.getFileId(),fileInfo);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * 保存元数据到文件
     *
     * 实现步骤：
     * 1. 获取读锁 lock.readLock().lock()
     * 2. 将 fileInfoMap.values() 转换为 List
     * 3. 创建 File 对象
     * 4. 确保父目录存在（getParentFile().mkdirs()）
     * 5. 使用 objectMapper 写入 JSON 文件
     * 6. 释放读锁
     * 7. 捕获异常打印错误日志
     */
    private void saveMetadata() {
        // TODO: 将元数据保存到 JSON 文件
        lock.readLock().lock();
        try {
            List<FileInfo>fileInfoList=new ArrayList<>(fileInfoMap.values());
            File file=new File(metadataPath);
            File parentFDir=file.getParentFile();
            if(parentFDir!=null && !parentFDir.exists()){
                parentFDir.mkdirs();
            }
            objectMapper.writeValue(file,fileInfoList);

        } catch (IOException e) {
            System.err.println("保存元数据失败:"+e.getMessage());
        }finally {
            lock.readLock().unlock();
        }


    }

    // ========== 文件上传 ==========

    /**
     * 初始化文件上传（创建文件元信息）
     * @param fileName 原始文件名
     * @param fileSize 文件大小
     * @param uploaderId 上传者ID
     * @param uploaderName 上传者昵称
     * @return FileInfo 对象，包含生成的 fileId 和存储路径
     *
     * 实现步骤：
     * 1. 生成 fileId = UUID.randomUUID().toString()
     * 2. 提取文件扩展名（最后一个点之后的内容）
     * 3. 生成存储路径（使用 fileStorage.generatePath()）
     * 4. 计算总分块数（使用 fileStorage.calculateTotalChunks()）
     * 5. 创建 FileInfo 对象
     * 6. 获取写锁
     * 7. 存入 fileInfoMap
     * 8. 释放写锁
     * 9. 调用 saveMetadata() 持久化
     * 10. 返回 FileInfo
     */
    public FileInfo initUpload(String fileName, long fileSize, String uploaderId, String uploaderName) {
        // TODO: 初始化文件上传，返回 FileInfo
        String fileId= null;
        String extension = null;
        String path = null;
        int totalChunks = 0;
        try {
            fileId = UUID.randomUUID().toString();
            //找到最后一个点的位置
            int lastDotIndex=fileName.lastIndexOf(".");
            //获取点之后的内容
            extension = (lastDotIndex>=0) ? fileName.substring(lastDotIndex+1):"";

            path = fileStorage.generatePath(fileId, fileName);
            totalChunks = fileStorage.calculateTotalChunks(fileSize, CHUNK_SIZE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        FileInfo fileInfo = new FileInfo(
                fileId,           // fileId
                fileName,         // name
                fileSize,         // fileSize
                uploaderId,       // uploaderId
                uploaderName,     // uploaderName
                LocalDateTime.now().toString(),  // uploadTime
                path,             // savePath
                extension,        // fileType
                totalChunks,      // totalChunks
                0,                // transferredBytes（初始为0）
                false             // isComplete（未完成）
        );
        lock.writeLock().lock();
        try {
            fileInfoMap.put(fileId,fileInfo);
            saveMetadata();
            return fileInfo;
        } finally {
            lock.writeLock().unlock();
        }




    }

    /**
     * 上传文件分块（支持断点续传）
     * @param fileId 文件ID
     * @param chunkIndex 分块索引（从0开始）
     * @param chunkData 分块数据
     * @return 是否上传成功
     * 实现步骤：
     * 1. 从 fileInfoMap 获取 fileInfo，不存在返回 false
     * 2. 如果文件已完成，返回 false
     * 3. 调用 fileStorage.saveChunk() 保存分块
     * 4. 更新 transferredBytes：增加 chunkData.length
     * 5. 判断是否完成：transferredBytes >= fileSize
     * 6. 如果完成，设置 isComplete = true
     * 7. 获取写锁
     * 8. 更新 fileInfoMap 中的 fileInfo
     * 9. 释放写锁
     * 10. 调用 saveMetadata() 持久化
     * 11. 返回 true
     */
    public boolean uploadChunk(String fileId, int chunkIndex, byte[] chunkData) {
        // TODO: 上传文件分块
        FileInfo fileInfo = fileInfoMap.get(fileId);
        if(fileInfo==null){
            return false;
        }
        boolean saved = fileStorage.saveChunk(fileInfo.getSavePath(), chunkData, chunkIndex, fileInfo.getTotalChunks());
        if (!saved) {
            return false;
        }
        fileInfo.setTransferredBytes(fileInfo.getTransferredBytes() + chunkData.length);

        fileInfo.setComplete(fileInfo.getTransferredBytes() >= fileInfo.getFileSize());

        lock.writeLock().lock();
        try {
            fileInfoMap.put(fileId,fileInfo);
            saveMetadata();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            lock.writeLock().unlock();
        }





        return true;
    }

    /**
     * 一键上传小文件（不分块，直接上传）
     * @param fileName 文件名
     * @param fileData 完整文件数据
     * @param uploaderId 上传者ID
     * @param uploaderName 上传者昵称
     * @return 是否上传成功
     * 实现步骤：
     * 1. 调用 initUpload() 初始化
     * 2. 调用 fileStorage.saveFileData() 保存完整数据
     * 3. 更新 transferredBytes 和 isComplete
     * 4. 保存元数据
     * 5. 返回成功或失败
     */
    public boolean uploadSmallFile(String fileName, byte[] fileData, String uploaderId, String uploaderName) {
        // TODO: 上传小文件（不分块）
       FileInfo fileInfo=initUpload(fileName,fileData.length,uploaderId,uploaderName);
        fileInfo.setUploadTime(LocalDateTime.now().toString());
       //保存文件数据(不需要锁)
        boolean saved= fileStorage.saveFileData(fileInfo.getSavePath(),fileData,false);
        if(!saved){
            // 保存失败，删除刚创建的元数据
            deleteFile(fileInfo.getFileId());
            return false;
        }
        //更新状态加锁
        lock.writeLock().lock();
        try {
            fileInfo.setTransferredBytes(fileInfo.getFileSize());
            fileInfo.setComplete(true);
            saveMetadata();
        } finally {
            lock.writeLock().unlock();
        }
        return true;
    }

    // ========== 文件下载 ==========

    /**
     * 获取文件信息（用于下载前获取元数据）
     * @param fileId 文件ID
     * @return 文件元信息，不存在返回 null
     */
    public FileInfo getFileInfo(String fileId) {
        // TODO: 从 fileInfoMap 获取 fileInfo
        return fileInfoMap.get(fileId);
    }

    /**
     * 下载文件分块
     * @param fileId 文件ID
     * @param chunkIndex 分块索引
     * @return 分块数据，失败返回 null
     *
     * 实现步骤：
     * 1. 从 fileInfoMap 获取 fileInfo
     * 2. 如果不存在或未完成，返回 null
     * 3. 计算起始位置：chunkIndex * CHUNK_SIZE
     * 4. 计算读取长度：最小(CHUNK_SIZE, fileSize - 起始位置)
     * 5. 调用 fileStorage.readFileData() 读取
     * 6. 返回读取的数据
     */
    public byte[] downloadChunk(String fileId, int chunkIndex) {
        // TODO: 下载文件分块
        FileInfo fileInfo = fileInfoMap.get(fileId);
        if(fileInfo==null || !fileInfo.isComplete()){
            return null;
        }
        int firstStart=chunkIndex*CHUNK_SIZE;
        int readLength= Math.toIntExact(Math.min(CHUNK_SIZE, fileInfo.getFileSize() - firstStart));
        return fileStorage.readFileData(fileInfo.getSavePath(), firstStart, readLength);
    }

    /**
     * 下载完整文件（小文件）
     * @param fileId 文件ID
     * @return 完整文件数据，失败返回 null
     *
     * 实现步骤：
     * 1. 调用 getFileInfo() 获取元信息
     * 2. 调用 fileStorage.readFileData() 读取整个文件
     * 3. 返回数据
     */
    public byte[] downloadFullFile(String fileId) {
        // TODO: 下载完整文件
        FileInfo fileInfo = fileInfoMap.get(fileId);
        if(fileInfo==null || !fileInfo.isComplete()){
            return null;
        }
        try {
            return fileStorage.readFileData(fileInfo.getSavePath(),0,(int)fileInfo.getFileSize());
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * 获取下载进度（用于断点续传）
     * @param fileId 文件ID
     * @return 已下载的字节数，失败返回 -1
     *
     * 实现步骤：
     * 1. 检查本地是否已有部分文件
     * 2. 调用 fileStorage.getFileSize() 获取已下载大小
     * 3. 返回已下载大小
     *
     * 注意：此方法用于客户端断点续传，询问服务器已传输多少
     */
    public long getDownloadProgress(String fileId) {
        // TODO: 获取已下载进度
        FileInfo fileInfo = fileInfoMap.get(fileId);
        if(fileInfo==null){
            return -1;
        }
        try {
            return fileStorage.getFileSize(fileInfo.getSavePath());
        } catch (Exception e) {
            return -1;
        }
    }

    // ========== 文件列表查询 ==========

    /**
     * 获取所有已完成文件列表
     *
     * 实现步骤：
     * 1. 获取读锁
     * 2. 使用 Stream 过滤 isComplete == true
     * 3. 收集为 List 返回
     */
    public List<FileInfo> getAllCompletedFiles() {
        lock.readLock().lock();
        try {
            return fileInfoMap.values()
                    .stream()
                    .filter(FileInfo::isComplete)
                    .sorted((a, b) -> {
                        String ta = a.getUploadTime() != null ? a.getUploadTime() : "";
                        String tb = b.getUploadTime() != null ? b.getUploadTime() : "";
                        return tb.compareTo(ta);  // 新的在前
                    })
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取所有文件列表（包括未完成的）
     *
     * 实现步骤：
     * 1. 获取读锁
     * 2. 返回 new ArrayList<>(fileInfoMap.values())
     */
    public List<FileInfo> getAllFiles() {
        // TODO: 返回所有文件列表
        lock.readLock().lock();
        try {
            return new ArrayList<>(fileInfoMap.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取指定用户上传的文件列表
     * @param uploaderName 上传者昵称
     *
     * 实现步骤：
     * 1. 获取读锁
     * 2. 使用 Stream 过滤 uploaderName 匹配的文件
     * 3. 收集为 List 返回
     */
    public List<FileInfo> getFilesByUser(String uploaderName) {
        // TODO: 根据上传者筛选文件列表
        lock.readLock().lock();
        try {
            return fileInfoMap.values()
                    .stream()
                    .filter(s->s.getUploaderName().equals(uploaderName))
                    .toList();
        } finally {
            lock.readLock().unlock();
        }

    }

    // ========== 文件管理 ==========

    /**
     * 删除文件
     * @param fileId 文件ID
     * @return 是否删除成功
     *
     * 实现步骤：
     * 1. 获取写锁
     * 2. 从 fileInfoMap 获取 fileInfo
     * 3. 如果不存在，返回 false
     * 4. 调用 fileStorage.deleteFile(fileInfo.getSavePath())
     * 5. 从 fileInfoMap 中移除
     * 6. 释放写锁
     * 7. 调用 saveMetadata() 持久化
     * 8. 返回 true
     */
    public boolean deleteFile(String fileId) {
        // TODO: 删除文件
        lock.writeLock().lock();
        try {
            FileInfo fileInfo = fileInfoMap.get(fileId);
            if(fileInfo==null){
                return false;
            }
            //物理删除文件
            boolean deleted=fileStorage.deleteFile(fileInfo.getSavePath());
            //无论是否成功,都从元数据中移除
            fileInfoMap.remove(fileId);
            saveMetadata();
            return true;
        } catch (Exception e) {
            return false;
        }finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * 获取存储使用情况
     * @return 格式化的存储信息（如 "已使用 125.5 MB / 总共 1 GB"）
     *
     * 实现步骤：
     * 1. 遍历所有已完成文件，累加 fileSize
     * 2. 格式化返回
     */
    public String getStorageUsage() {
        // TODO: 获取存储使用情况
        long totalBytes=0;
        for (FileInfo value : fileInfoMap.values()) {
            if(value.isComplete()){
                totalBytes+=value.getFileSize();
            }
        }
        // 格式化显示
        if (totalBytes < 1024) {
            return "已使用 " + totalBytes + " B";
        } else if (totalBytes < 1024 * 1024) {
            return "已使用 " + String.format("%.2f", totalBytes / 1024.0) + " KB";
        } else if (totalBytes < 1024 * 1024 * 1024) {
            return "已使用 " + String.format("%.2f", totalBytes / (1024.0 * 1024.0)) + " MB";
        } else {
            return "已使用 " + String.format("%.2f", totalBytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
        }
    }
}