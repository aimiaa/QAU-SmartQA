package com.aimi.mapper;

import com.aimi.entity.DocumentChunkEntity;
import com.aimi.entity.DocumentChunkMatch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunkEntity> {

    /** 批量写入切片，embedding 需 ::vector 转换，SQL 见 mapper/DocumentChunkMapper.xml。 */
    int insertBatch(@Param("chunks") List<DocumentChunkEntity> chunks);

    /** 重解析前物理清理旧切片（切片表不做逻辑删除，向量数据留着无意义）。 */
    int deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * 基于 pgvector 余弦相似度检索最相关的切片，SQL 见 mapper/DocumentChunkMapper.xml。
     *
     * @param queryVector      查询向量字面量，形如 "[0.1,0.2,...]"
     * @param knowledgeBaseIds 限定检索的知识库范围，为空则全库检索
     * @param topK             返回条数上限
     * @return 按相关度降序的切片列表
     */
    List<DocumentChunkMatch> searchSimilarChunks(@Param("queryVector") String queryVector,
                                                 @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
                                                 @Param("topK") int topK);

    /**
     * 关键词（pg_trgm 词相似度）检索，与向量检索组成混合召回，SQL 见 mapper/DocumentChunkMapper.xml。
     *
     * @param keyword          查询关键词（原始或改写后的问题）
     * @param knowledgeBaseIds 限定检索的知识库范围，为空则全库检索
     * @param topK             返回条数上限
     * @return 按词相似度降序的切片列表
     */
    List<DocumentChunkMatch> searchByKeyword(@Param("keyword") String keyword,
                                             @Param("knowledgeBaseIds") List<Long> knowledgeBaseIds,
                                             @Param("topK") int topK);
}
