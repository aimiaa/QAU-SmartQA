package com.aimi.converter;

import com.aimi.dto.knowledge.KnowledgeBaseQueryDTO;
import com.aimi.entity.KnowledgeBaseEntity;
import com.aimi.vo.knowledge.KnowledgeBaseVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 知识库对象转换器：entity / DTO / VO 之间的转换与展示文案统一收口在这里。
 * Service 只负责业务编排，不再散落字段映射与格式化逻辑。
 */
@Component
public class KnowledgeBaseConverter {

    // 更新时间固定展示为“月日 时分”，不使用“今天/昨天”这类相对文案
    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final String EMPTY_UPDATED_AT = "暂无更新";

    /**
     * 归一化查询条件：空白值按未传处理，避免把 "  " 当成有效筛选条件。
     *
     * @param query 前端传入的原始查询条件，可为 null
     * @return 字段已去除首尾空白的查询条件，永不为 null
     */
    public KnowledgeBaseQueryDTO normalizeQuery(KnowledgeBaseQueryDTO query) {
        if (query == null) {
            return new KnowledgeBaseQueryDTO(null, null, null);
        }

        return new KnowledgeBaseQueryDTO(
                normalizeText(query.keyword()),
                normalizeText(query.status()),
                normalizeText(query.category())
        );
    }

    /**
     * 实体转列表 VO，同时兜住前端不允许为 null 的字段。
     *
     * @param entity 知识库实体，可为 null
     * @return 展示用 VO，入参为 null 时返回 null
     */
    public KnowledgeBaseVO toVO(KnowledgeBaseEntity entity) {
        if (entity == null) {
            return null;
        }

        return new KnowledgeBaseVO(
                entity.getId(),
                entity.getName(),
                entity.getCategory(),
                entity.getStatus(),
                // 前端会对 documents 求和，不能下发 null
                entity.getDocumentCount() == null ? 0 : entity.getDocumentCount(),
                formatUpdatedAt(entity.getUpdatedAt()),
                // 前端直接对 description 调用 toLowerCase，不能下发 null
                entity.getDescription() == null ? "" : entity.getDescription()
        );
    }

    /**
     * 批量转换，保持入参顺序。
     *
     * @param entities 知识库实体列表，可为 null
     * @return VO 列表，永不返回 null
     */
    public List<KnowledgeBaseVO> toVOList(List<KnowledgeBaseEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .map(this::toVO)
                .toList();
    }

    private String formatUpdatedAt(LocalDateTime updatedAt) {
        return updatedAt == null ? EMPTY_UPDATED_AT : updatedAt.format(UPDATED_AT_FORMATTER);
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
