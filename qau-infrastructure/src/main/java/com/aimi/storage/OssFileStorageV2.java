package com.aimi.storage;

import com.aimi.exception.BusinessException;
import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.OSSClientBuilder;
import com.aliyun.sdk.service.oss2.credentials.CredentialsProvider;
import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider;
import com.aliyun.sdk.service.oss2.models.GetObjectRequest;
import com.aliyun.sdk.service.oss2.models.GetObjectResult;
import com.aliyun.sdk.service.oss2.models.DeleteObjectRequest;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.models.PutObjectResult;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 阿里云 OSS 存储实现（SDK V2）：用于生产环境，替代本地磁盘存储。
 * 通过 app.knowledge.storage.type=oss 激活；默认仍走本地 LocalFileStorageService。
 * AK/SK 由 Spring 从 .env 注入（oss.access-key-id / oss.access-key-secret），不在代码中硬编码。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.knowledge.storage.type", havingValue = "oss")
public class OssFileStorageV2 implements FileStorageService {

    private static final String SCHEME = "oss://";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    private final CredentialsProvider credentialsProvider;
    private final String region;
    private final String bucket;

    public OssFileStorageV2(
            @Value("${oss.region:cn-hangzhou}") String region,
            @Value("${oss.bucket-name}") String bucket,
            @Value("${oss.access-key-id}") String accessKeyId,
            @Value("${oss.access-key-secret}") String accessKeySecret) {
        this.region = region;
        this.bucket = bucket;
        this.credentialsProvider = new StaticCredentialsProvider(accessKeyId, accessKeySecret);
    }

    @Override
    public String store(byte[] content, String extension) {
        String dir = LocalDate.now().format(DATE_FORMATTER);
        String suffix = (extension == null || extension.isBlank()) ? "" : "." + extension.toLowerCase();
        String key = dir + "/" + UUID.randomUUID() + suffix;

        OSSClientBuilder clientBuilder = OSSClient.newBuilder()
                .credentialsProvider(credentialsProvider)
                .region(region);

        try (OSSClient client = clientBuilder.build()) {
            PutObjectResult result = client.putObject(PutObjectRequest.newBuilder()
                    .bucket(bucket)
                    .key(key)
                    .body(BinaryData.fromBytes(content))
                    .build());
            log.info("OSS upload success, key={}, statusCode={}, requestId={}, eTag={}",
                    key, result.statusCode(), result.requestId(), result.eTag());
            return SCHEME + key;
        } catch (Exception e) {
            log.error("文件上传到 OSS 失败, key={}, error={}", key, e.getMessage(), e);
            throw new BusinessException("文件上传到 OSS 失败：" + e.getMessage(), e);
        }
    }

    @Override
    public byte[] retrieve(String storageUrl) {
        if (storageUrl == null || !storageUrl.startsWith(SCHEME)) {
            throw new BusinessException("非法的 OSS 地址：" + storageUrl);
        }
        String key = storageUrl.substring(SCHEME.length());

        OSSClientBuilder clientBuilder = OSSClient.newBuilder()
                .credentialsProvider(credentialsProvider)
                .region(region);

        try (OSSClient client = clientBuilder.build()) {
            GetObjectResult result = client.getObject(GetObjectRequest.newBuilder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            return result.body().readAllBytes();
        } catch (Exception e) {
            log.error("从 OSS 下载文件失败, key={}, error={}", key, e.getMessage(), e);
            throw new BusinessException("从 OSS 下载文件失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storageUrl) {
        if (storageUrl == null || !storageUrl.startsWith(SCHEME)) {
            throw new BusinessException("非法的 OSS 地址：" + storageUrl);
        }
        String key = storageUrl.substring(SCHEME.length());

        OSSClientBuilder clientBuilder = OSSClient.newBuilder()
                .credentialsProvider(credentialsProvider)
                .region(region);

        try (OSSClient client = clientBuilder.build()) {
            client.deleteObject(DeleteObjectRequest.newBuilder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            log.info("OSS object deleted, key={}", key);
        } catch (Exception e) {
            log.error("删除 OSS 文件失败, key={}, error={}", key, e.getMessage(), e);
            throw new BusinessException("删除 OSS 文件失败：" + e.getMessage(), e);
        }
    }
}
