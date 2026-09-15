package com.aimi.service.impl;

import com.aimi.converter.KnowledgeBaseConverter;
import com.aimi.dto.knowledge.KnowledgeBaseQueryDTO;
import com.aimi.mapper.KnowledgeBaseMapper;
import com.aimi.service.KnowledgeService;
import com.aimi.vo.knowledge.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    /** 停用状态：前端 KnowledgeBaseStatus 与 statusMeta 未定义该值，返回会导致徽标渲染取不到配置，故列表不下发。 */
    private static final String STATUS_DISABLED = "disabled";

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseConverter knowledgeBaseConverter;

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseVO> listKnowledgeBases(KnowledgeBaseQueryDTO query) {
        KnowledgeBaseQueryDTO condition = knowledgeBaseConverter.normalizeQuery(query);

        // 动态条件与排序均在 KnowledgeBaseMapper.xml 中维护，这里只做业务编排与对象转换
        List<KnowledgeBaseVO> result = knowledgeBaseConverter.toVOList(
                knowledgeBaseMapper.selectKnowledgeBaseList(
                        condition.keyword(), condition.status(), condition.category(), STATUS_DISABLED));

        log.info("Listed knowledge bases, keyword={}, status={}, category={}, size={}",
                condition.keyword(), condition.status(), condition.category(), result.size());
        return result;
    }

}
