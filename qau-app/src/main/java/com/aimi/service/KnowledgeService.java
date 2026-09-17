package com.aimi.service;

import com.aimi.dto.knowledge.KnowledgeBaseDTO;
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

    /**
     * 新建知识库，只创建基础信息，文档通过上传接口单独加入。
     *
     * @param dto 前端新建入参
     * @return 创建后的知识库 VO
     */
    KnowledgeBaseVO createKnowledgeBase(KnowledgeBaseDTO dto);
}
