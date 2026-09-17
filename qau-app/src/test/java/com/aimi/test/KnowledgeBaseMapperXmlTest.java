package com.aimi.test;

import com.aimi.dto.knowledge.KnowledgeBaseQueryDTO;
import com.aimi.entity.KnowledgeBaseEntity;
import com.aimi.mapper.KnowledgeBaseMapper;
import com.aimi.service.KnowledgeService;
import com.aimi.vo.knowledge.KnowledgeBaseVO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 KnowledgeBaseMapper.xml 的动态 SQL 能真实绑定执行，覆盖 <sql> 片段内的参数引用。
 * 依赖本地 PostgreSQL（docker compose 的 qau-postgres）与 V4 种子数据。
 */
@SpringBootTest
@ActiveProfiles("test")
class KnowledgeBaseMapperXmlTest {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private KnowledgeService knowledgeService;

    @Test
    void shouldQueryKnowledgeBasesWithDynamicConditions() {
        List<KnowledgeBaseEntity> all = knowledgeBaseMapper
                .selectKnowledgeBaseList(null, null, null, "disabled");
        assertFalse(all.isEmpty(), "无条件查询应能取到种子数据");

        // 关键词与状态同时生效：只有“研究生培养与学位”既是 ready 又含“研究”
        List<KnowledgeBaseEntity> filtered = knowledgeBaseMapper
                .selectKnowledgeBaseList("研究", "ready", null, "disabled");
        assertTrue(filtered.stream().anyMatch(item -> "研究生培养与学位".equals(item.getName())));

        List<KnowledgeBaseVO> voList = knowledgeService
                .listKnowledgeBases(new KnowledgeBaseQueryDTO(null, null, "教务"));
        assertEquals(1, voList.size());
        assertEquals(86, voList.get(0).documents().intValue());
        // 更新时间统一为“月日 时分”，不再输出今天/昨天文案
        assertTrue(voList.get(0).updatedAt().matches("\\d{2}-\\d{2} \\d{2}:\\d{2}"),
                "updatedAt 应为 MM-dd HH:mm，实际为 " + voList.get(0).updatedAt());
    }
}
