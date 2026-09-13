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
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeBaseId;
    private Long documentId;
    private Integer chunkIndex;
    private String title;
    private String content;
    private Integer tokenCount;
    private Integer pageNo;
    private String sectionTitle;

    /** pgvector 字段，实际项目建议用自定义 TypeHandler 映射 float[] 或 List<Float>。 */
    private String embedding;

    /** PostgreSQL jsonb 字段，实际项目可映射为 JsonNode、Map 或 String。 */
    @TableField("metadata_json")
    private String metadataJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Boolean deleted;
}
