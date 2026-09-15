package com.aimi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 知识库同步任务：解析文档、生成向量、全量同步。 */
@Data
@TableName("knowledge_sync_job")
public class KnowledgeSyncJobEntity {
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 目标知识库 ID
    private Long knowledgeBaseId;
    // 目标文档 ID（全量同步时可为空）
    private Long documentId;
    // 任务类型：parse 解析 / embed 向量化 / full_sync 全量同步
    private String jobType;
    // 任务状态：pending 待处理 / running 运行中 / success 成功 / failed 失败
    private String status;
    // 进度百分比（0-100）
    private Integer progress;
    // 失败时的错误信息
    private String errorMessage;
    // 任务开始时间
    private LocalDateTime startedAt;
    // 任务结束时间
    private LocalDateTime finishedAt;
    // 创建人用户 ID
    private Long createdBy;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间（由数据库触发器自动维护）
    private LocalDateTime updatedAt;
}
