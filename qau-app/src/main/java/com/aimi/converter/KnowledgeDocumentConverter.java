package com.aimi.converter;

import com.aimi.entity.KnowledgeDocumentEntity;
import com.aimi.vo.knowledge.KnowledgeDocumentVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** 知识库文档对象转换：DB 状态与前端状态文案在此收口映射。 */
@Component
public class KnowledgeDocumentConverter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final String DOC_VECTORIZED = "vectorized";
    /** 前端 KnowledgeDocumentStatus 只认 ready，DB 终态 vectorized 需映射。 */
    private static final String FRONTEND_READY = "ready";

    public KnowledgeDocumentVO toVO(KnowledgeDocumentEntity entity) {
        if (entity == null) {
            return null;
        }
        String status = DOC_VECTORIZED.equals(entity.getStatus())
                ? FRONTEND_READY
                : entity.getStatus();

        return new KnowledgeDocumentVO(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getTitle(),
                entity.getFileName(),
                entity.getFileType(),
                entity.getFileSize(),
                status,
                formatTime(entity.getUploadedAt()),
                formatTime(entity.getParsedAt())
        );
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }
}
