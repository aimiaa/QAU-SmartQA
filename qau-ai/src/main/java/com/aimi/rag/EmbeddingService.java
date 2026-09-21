package com.aimi.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 向量化服务：封装 Spring AI 的 EmbeddingModel（DashScope OpenAI 兼容接口）。
 * 模型不可用时抛异常，由调用方决定降级策略（本项目解析流程会跳过向量继续保存切片）。
 */
@Service
public class EmbeddingService {

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    public EmbeddingService(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.embeddingModelProvider = embeddingModelProvider;
    }

    public float[] embed(String text) {
        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null) {
            throw new IllegalStateException("未配置可用的 embedding 模型，请检查 spring.ai.model.embedding");
        }
        return model.embed(text);
    }

    /**
     * 向量化并序列化为 pgvector 字面量 "[v1,v2,...]"，用于 ::vector 相似度检索。
     * 与文档写库时的向量格式保持一致。
     */
    public String embedToVectorLiteral(String text) {
        return toVectorLiteral(embed(text));
    }

    private String toVectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(vector.length * 8).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}