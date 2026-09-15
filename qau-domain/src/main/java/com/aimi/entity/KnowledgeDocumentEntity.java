package com.aimi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 知识库文档：PDF、Word、通知附件等原始文件元数据。 */
@Data
@TableName("knowledge_document")
public class KnowledgeDocumentEntity {
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 所属知识库 ID
    private Long knowledgeBaseId;
    // 文档标题
    private String title;
    // 原始文件名
    private String fileName;
    // 文件类型（如 pdf、docx）
    private String fileType;
    // 文件大小（字节）
    private Long fileSize;
    // 原始文件在 MinIO / OSS 中的存储地址
    private String storageUrl;
    // 文件校验码（用于去重与完整性校验）
    private String checksum;
    // 状态：uploaded 已上传 / parsing 解析中 / vectorized 已向量化 / review 待复核 / failed 失败 / disabled 停用
    private String status;
    // 版本号
    private Integer versionNo;
    // 上传人用户 ID
    private Long uploadedBy;
    // 上传时间
    private LocalDateTime uploadedAt;
    // 解析完成时间
    private LocalDateTime parsedAt;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间（由数据库触发器自动维护）
    private LocalDateTime updatedAt;
    // 逻辑删除标记
    @TableLogic
    private Boolean deleted;
}
