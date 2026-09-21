package com.aimi.mapper;

import com.aimi.entity.KnowledgeDocumentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentEntity> {

    /** Lock an active document while its asynchronous parse task is processing it. */
    @Select("""
            SELECT * FROM knowledge_document
            WHERE id = #{documentId} AND deleted = FALSE
            FOR UPDATE
            """)
    KnowledgeDocumentEntity selectActiveForUpdate(@Param("documentId") Long documentId);

    /** Count active documents so an empty knowledge base can return to the building state. */
    @Select("""
            SELECT COUNT(*) FROM knowledge_document
            WHERE knowledge_base_id = #{kbId} AND deleted = FALSE
            """)
    long countActive(@Param("kbId") Long kbId);

    /** 统计仍在处理中的文档数，终态 = vectorized / failed / review。 */
    @Select("""
            SELECT COUNT(*) FROM knowledge_document
            WHERE knowledge_base_id = #{kbId} AND deleted = FALSE
              AND status NOT IN ('vectorized', 'failed', 'review')
            """)
    long countUnfinished(@Param("kbId") Long kbId);

    /** 统计解析失败的文档数。 */
    @Select("""
            SELECT COUNT(*) FROM knowledge_document
            WHERE knowledge_base_id = #{kbId} AND deleted = FALSE AND status = 'failed'
            """)
    long countFailed(@Param("kbId") Long kbId);
}
