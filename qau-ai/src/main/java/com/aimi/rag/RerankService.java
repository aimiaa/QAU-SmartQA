package com.aimi.rag;

import com.aimi.entity.DocumentChunkMatch;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 重排序服务：调用 DashScope 文本排序模型（默认 gte-rerank-v2）对融合后的候选片段二次排序。
 * 复用 DASHSCOPE_API_KEY；未开启、无 Key 或调用失败时按传入顺序原样返回，不阻断问答主流程。
 */
@Slf4j
@Service
public class RerankService {

    private static final String RERANK_PATH = "/services/rerank/text-rerank/text-rerank";

    private final RestClient restClient;
    private final boolean available;
    private final String model;
    private final double minScore;

    public RerankService(
            @Value("${app.rag.rerank.enabled:true}") boolean enabled,
            @Value("${app.rag.rerank.model:gte-rerank-v2}") String model,
            @Value("${app.rag.rerank.base-url:https://dashscope.aliyuncs.com/api/v1}") String baseUrl,
            @Value("${app.rag.rerank.api-key:}") String apiKey,
            @Value("${app.rag.rerank.min-score:0.1}") double minScore,
            @Value("${app.rag.rerank.timeout-ms:5000}") int timeoutMs) {
        this.model = model;
        this.minScore = minScore;
        this.available = enabled && apiKey != null && !apiKey.isBlank();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();

        if (enabled && !this.available) {
            log.warn("Rerank 已开启但未配置 app.rag.rerank.api-key（DASHSCOPE_API_KEY），将跳过重排序。");
        }
    }

    /**
     * 对候选片段按与 query 的相关性重排。
     *
     * @param query      检索查询
     * @param candidates 融合后的候选片段（会被就地写入 rerank 分数到 similarity）
     * @return 重排并过滤低分后的片段；不可用或失败时返回原候选
     */
    public List<DocumentChunkMatch> rerank(String query, List<DocumentChunkMatch> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        if (!available || candidates.size() == 1) {
            return candidates;
        }
        try {
            List<String> documents = candidates.stream().map(DocumentChunkMatch::getContent).toList();
            RerankResponse response = restClient.post()
                    .uri(RERANK_PATH)
                    .body(new RerankRequest(model, new RerankInput(query, documents),
                            new RerankParameters(false, candidates.size())))
                    .retrieve()
                    .body(RerankResponse.class);

            if (response == null || response.output() == null
                    || response.output().results() == null || response.output().results().isEmpty()) {
                return candidates;
            }

            List<DocumentChunkMatch> reranked = new ArrayList<>(candidates.size());
            for (RerankResult r : response.output().results()) {
                if (r.index() < 0 || r.index() >= candidates.size()) {
                    continue;
                }
                DocumentChunkMatch m = candidates.get(r.index());
                m.setSimilarity(r.relevanceScore());
                if (m.getSimilarity() == null || m.getSimilarity() >= minScore) {
                    reranked.add(m);
                }
            }
            return reranked;
        } catch (Exception e) {
            log.warn("Rerank 调用失败，降级为融合顺序。query={}", query, e);
            return candidates;
        }
    }

    private record RerankRequest(String model, RerankInput input, RerankParameters parameters) {}

    private record RerankInput(String query, List<String> documents) {}

    private record RerankParameters(
            @JsonProperty("return_documents") boolean returnDocuments,
            @JsonProperty("top_n") int topN) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RerankResponse(RerankOutput output) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RerankOutput(List<RerankResult> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RerankResult(int index, @JsonProperty("relevance_score") Double relevanceScore) {}
}