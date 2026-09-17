package com.aimi.storage;

import com.aimi.exception.BusinessException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 本地磁盘存储实现：OSS 未配置时的默认方案，保证联调开箱可用。
 * 通过 app.knowledge.storage.type=local（缺省即本地）激活；后续接入 OSS 只需另建实现类并把类型设为 oss。
 */
@Service
@ConditionalOnProperty(name = "app.knowledge.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final String SCHEME = "local://";

    private final Path root;

    public LocalFileStorageService(
            @Value("${app.knowledge.storage.local-dir:./data/knowledge-files}") String localDir) {
        this.root = Paths.get(localDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new UncheckedIOException("无法创建本地存储目录: " + this.root, e);
        }
    }

    @Override
    public String store(byte[] content, String extension) {
        String suffix = (extension == null || extension.isBlank()) ? "" : "." + extension.toLowerCase();
        String fileName = UUID.randomUUID() + suffix;
        Path target = root.resolve(fileName);
        try {
            Files.write(target, content);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage(), e);
        }
        return SCHEME + fileName;
    }

    @Override
    public byte[] retrieve(String storageUrl) {
        if (storageUrl == null || !storageUrl.startsWith(SCHEME)) {
            throw new BusinessException("非法的存储地址：" + storageUrl);
        }
        String fileName = storageUrl.substring(SCHEME.length());
        Path target = root.resolve(fileName).normalize();
        // 防目录穿越
        if (!target.startsWith(root)) {
            throw new BusinessException("非法的文件路径");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new BusinessException("文件读取失败：" + e.getMessage(), e);
        }
    }
}