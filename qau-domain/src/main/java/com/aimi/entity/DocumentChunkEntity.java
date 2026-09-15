package com.aimi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 文档切片：RAG 检索的最小文本块。 */
@Data
@TableName("document_chunk")
public class DocumentChunkEntity {
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 所属知识库 ID
    private Long knowledgeBaseId;
    // 所属文档 ID
    private Long documentId;
    // 切片序号（同一文档内唯一）
    private Integer chunkIndex;
    // 切片标题
    private String title;
    // 切片正文内容
    private String content;
    // token 数
    private Integer tokenCount;
    // 页码
    private Integer pageNo;
    // 章节标题
    private String sectionTitle;

    /** pgvector 字段，实际项目建议用自定义 TypeHandler 映射 float[] 或 List<Float>。 */
    private String embedding;

    /** PostgreSQL jsonb 字段，实际项目可映射为 JsonNode、Map 或 String。 */
    @TableField("metadata_json")
    private String metadataJson;

    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间（由数据库触发器自动维护）
    private LocalDateTime updatedAt;
    // 逻辑删除标记
    @TableLogic
    private Boolean deleted;
}
