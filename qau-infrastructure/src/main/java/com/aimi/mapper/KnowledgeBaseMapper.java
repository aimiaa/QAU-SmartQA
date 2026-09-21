package com.aimi.mapper;

import com.aimi.entity.KnowledgeBaseEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {

    /**
     * 按条件查询知识库列表，动态 SQL 见 mapper/KnowledgeBaseMapper.xml。
     * 自定义 SQL 不会自动追加逻辑删除条件，XML 内已显式过滤 deleted。
     *
     * @param keyword       关键词，模糊匹配名称、分类、描述，为空不过滤
     * @param status        状态精确匹配，为空不过滤
     * @param category      分类精确匹配，为空不过滤
     * @param excludeStatus 需要排除的状态，为空不排除
     * @return 按更新时间倒序的知识库列表
     */
    List<KnowledgeBaseEntity> selectKnowledgeBaseList(@Param("keyword") String keyword,
                                                      @Param("status") String status,
                                                      @Param("category") String category,
                                                      @Param("excludeStatus") String excludeStatus);

    /** 文档数原子自增，避免"读-改-写"并发丢失。 */
    @Update("UPDATE knowledge_base SET document_count = document_count + 1 WHERE id = #{kbId} AND deleted = FALSE")
    int increaseDocumentCount(@Param("kbId") Long kbId);

    /**
     * 上传文档时同步知识库状态：置为 syncing 并原子自增文档数，
     * 避免“读-改-写”在并发上传时丢失计数。
     */
    @Update("""
            UPDATE knowledge_base
            SET status = 'syncing', document_count = document_count + 1
            WHERE id = #{kbId} AND deleted = FALSE
            """)
    int markSyncingAndIncreaseDocumentCount(@Param("kbId") Long kbId);

    /** Atomically decrease the denormalized document count after a document is deleted. */
    @Update("UPDATE knowledge_base SET document_count = GREATEST(document_count - 1, 0) WHERE id = #{kbId} AND deleted = FALSE")
    int decreaseDocumentCount(@Param("kbId") Long kbId);
}
