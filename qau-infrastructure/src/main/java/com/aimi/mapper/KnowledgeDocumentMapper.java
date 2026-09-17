package com.aimi.mapper;

import com.aimi.entity.KnowledgeDocumentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentEntity> {

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