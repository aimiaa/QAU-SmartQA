package com.aimi.service.impl;

import com.aimi.entity.DocumentChunkMatch;
import com.aimi.mapper.DocumentChunkMapper;
import com.aimi.rag.EmbeddingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * RAG 检索服务：把用户问题向量化后，在 document_chunk 上做 pgvector 余弦相似度检索，
 * 返回 Top-K 相关切片并拼接为可注入提示词的上下文。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private final DocumentChunkMapper documentChunkMapper;
    private final EmbeddingService embeddingService;

    /** 召回条数上限。 */
    @Value("${app.rag.top-k:5}")
    private int topK;

    /** 相似度阈值，低于该值视为不相关，避免弱相关片段污染答案。 */
    @Value("${app.rag.min-similarity:0.3}")
    private double minSimilarity;

    /**
     * 检索与问题最相关的切片。
     *
     * @param query            检索查询（原始问题或改写后的问题）
     * @param knowledgeBaseIds 限定知识库范围，为空则全库检索
     * @return 相似度达标的相关切片，按相关度降序；向量化不可用时返回空列表（触发兜底）
     */
    public List<DocumentChunkMatch> retrieve(String query, List<Long> knowledgeBaseIds) {
        String queryVector;
        try {
            queryVector = embeddingService.embedToVectorLiteral(query);
        } catch (Exception e) {
            log.warn("Embed query failed, skip retrieval. query={}", query, e);
            return List.of();
        }
        if (queryVector == null) {
            return List.of();
        }

        List<DocumentChunkMatch> matches =
                documentChunkMapper.searchSimilarChunks(queryVector, knowledgeBaseIds, topK);
        return matches.stream()
                .filter(m -> m.getSimilarity() != null && m.getSimilarity() >= minSimilarity)
                .toList();
    }

    /** 把检索到的切片拼接为提示词上下文，带来源标注便于模型区分片段边界。 */
    public String buildContext(List<DocumentChunkMatch> matches) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            DocumentChunkMatch m = matches.get(i);
            String source = m.getDocumentTitle() != null ? m.getDocumentTitle() : m.getFileName();
            sb.append("【片段").append(i + 1).append("｜来源：").append(source).append("】\n")
                    .append(m.getContent())
                    .append("\n\n");
        }
        return sb.toString().strip();
    }
}