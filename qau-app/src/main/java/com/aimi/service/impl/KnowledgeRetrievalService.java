package com.aimi.service.impl;

import com.aimi.entity.DocumentChunkMatch;
import com.aimi.mapper.DocumentChunkMapper;
import com.aimi.rag.EmbeddingService;
import com.aimi.rag.RerankService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * RAG 检索服务：用 pgvector 向量召回和 pg_trgm 关键词召回构建候选集，
 * 经 RRF 融合与重排序后返回 Top-K 相关切片，并拼接为可注入提示词的上下文。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private static final int RRF_K = 60;

    private final DocumentChunkMapper documentChunkMapper;
    private final EmbeddingService embeddingService;
    private final RerankService rerankService;

    /** 召回条数上限。 */
    @Value("${app.rag.top-k:5}")
    private int topK;

    /** 相似度阈值，低于该值视为不相关，避免弱相关片段污染答案。 */
    @Value("${app.rag.min-similarity:0.3}")
    private double minSimilarity;

    /** 向量召回候选池大小，融合/重排后再截断到 topK。 */
    @Value("${app.rag.vector-candidates:20}")
    private int vectorCandidates;

    /** 是否启用 pg_trgm 关键词召回。 */
    @Value("${app.rag.keyword.enabled:true}")
    private boolean keywordEnabled;

    /** 关键词召回候选池大小。 */
    @Value("${app.rag.keyword.candidates:20}")
    private int keywordCandidates;

    /** 关键词召回最低相似度，避免极弱命中进入融合。 */
    @Value("${app.rag.keyword.min-similarity:0.05}")
    private double keywordMinSimilarity;

    /**
     * 检索与问题最相关的切片。
     *
     * @param query            检索查询（原始问题或改写后的问题）
     * @param knowledgeBaseIds 限定知识库范围，为空则全库检索
     * @return 相似度达标的相关切片，按重排相关度降序；召回不可用时返回空列表（触发兜底）
     */
    public List<DocumentChunkMatch> retrieve(String query, List<Long> knowledgeBaseIds) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        List<DocumentChunkMatch> vectorMatches = searchByVector(query, knowledgeBaseIds);
        List<DocumentChunkMatch> keywordMatches = searchByKeyword(query, knowledgeBaseIds);
        List<DocumentChunkMatch> fused = reciprocalRankFusion(vectorMatches, keywordMatches);
        if (fused.isEmpty()) {
            return List.of();
        }

        return rerankService.rerank(query, fused).stream()
                .limit(topK)
                .toList();
    }

    private List<DocumentChunkMatch> searchByVector(String query, List<Long> knowledgeBaseIds) {
        try {
            String queryVector = embeddingService.embedToVectorLiteral(query);
            if (queryVector == null) {
                return List.of();
            }

            return documentChunkMapper.searchSimilarChunks(queryVector, knowledgeBaseIds, Math.max(vectorCandidates, topK))
                    .stream()
                    .filter(m -> m.getSimilarity() != null && m.getSimilarity() >= minSimilarity)
                    .toList();
        } catch (Exception e) {
            log.warn("Vector retrieval failed, fallback to keyword retrieval only. query={}", query, e);
            return List.of();
        }
    }

    private List<DocumentChunkMatch> searchByKeyword(String query, List<Long> knowledgeBaseIds) {
        if (!keywordEnabled || query.isBlank()) {
            return List.of();
        }
        try {
            return documentChunkMapper.searchByKeyword(query, knowledgeBaseIds, Math.max(keywordCandidates, topK))
                    .stream()
                    .filter(m -> m.getSimilarity() != null && m.getSimilarity() >= keywordMinSimilarity)
                    .toList();
        } catch (Exception e) {
            log.warn("Keyword retrieval failed, fallback to vector retrieval only. query={}", query, e);
            return List.of();
        }
    }

    private List<DocumentChunkMatch> reciprocalRankFusion(
            List<DocumentChunkMatch> vectorMatches,
            List<DocumentChunkMatch> keywordMatches) {
        Map<Long, FusedChunk> fused = new LinkedHashMap<>();
        addRankScores(fused, vectorMatches);
        addRankScores(fused, keywordMatches);

        return fused.values().stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .map(FusedChunk::match)
                .toList();
    }

    private void addRankScores(Map<Long, FusedChunk> fused, List<DocumentChunkMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return;
        }
        for (int i = 0; i < matches.size(); i++) {
            DocumentChunkMatch match = matches.get(i);
            if (match.getId() == null) {
                continue;
            }
            double rankScore = 1.0 / (RRF_K + i + 1);
            fused.compute(match.getId(), (id, existing) -> {
                if (existing == null) {
                    return new FusedChunk(match, rankScore);
                }
                existing.addScore(rankScore);
                return existing;
            });
        }
    }

    private static final class FusedChunk {
        private final DocumentChunkMatch match;
        private double score;

        private FusedChunk(DocumentChunkMatch match, double score) {
            this.match = match;
            this.score = score;
            this.match.setSimilarity(score);
        }

        private DocumentChunkMatch match() {
            return match;
        }

        private double score() {
            return score;
        }

        private void addScore(double score) {
            this.score += score;
            this.match.setSimilarity(this.score);
        }
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
