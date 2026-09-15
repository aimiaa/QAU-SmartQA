package com.aimi.service;

import com.aimi.dto.knowledge.KnowledgeBaseQueryDTO;
import com.aimi.vo.knowledge.KnowledgeBaseVO;
import java.util.List;

public interface KnowledgeService {

    /**
     * 查询知识库列表，支持关键词、状态、分类筛选。
     *
     * @param query 查询条件，字段为空表示不参与过滤
     * @return 知识库列表，按更新时间倒序
     */
    List<KnowledgeBaseVO> listKnowledgeBases(KnowledgeBaseQueryDTO query);
}
