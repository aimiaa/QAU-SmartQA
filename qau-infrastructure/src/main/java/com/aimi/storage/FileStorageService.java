package com.aimi.storage;

/**
 * 文件存储服务：屏蔽底层存储介质（本地磁盘 / 阿里云 OSS）。
 * store 返回可回读的定位串（storageUrl），retrieve 依据该串读回原始字节。
 */
public interface FileStorageService {

    /**
     * 保存文件内容。
     * @param content   原始字节
     * @param extension 文件扩展名（不含点），可为空
     * @return 存储定位串，用于回写 knowledge_document.storage_url
     */
    String store(byte[] content, String extension);

    /**
     * 读取已存储文件。
     * @param storageUrl {@link #store} 返回的定位串
     * @return 原始字节
     */
    byte[] retrieve(String storageUrl);

    /** Delete the stored object after its document metadata has been removed. */
    void delete(String storageUrl);
}
