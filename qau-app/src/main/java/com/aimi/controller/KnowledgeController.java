package com.aimi.controller;

import com.aimi.dto.knowledge.KnowledgeBaseQueryDTO;
import com.aimi.result.Result;
import com.aimi.service.KnowledgeService;
import com.aimi.vo.knowledge.KnowledgeBaseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/knowledge-bases")
@Tag(name = "knowledge", description = "Knowledge base related operations")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /**
     * 查询知识库列表。
     * @param keyword  关键词，模糊匹配名称/分类/描述
     * @param status   状态精确筛选
     * @param category 分类精确筛选
     * @return 知识库列表
     */
    @Operation(summary = "查询知识库列表")
    @GetMapping
    public Result<List<KnowledgeBaseVO>> listKnowledgeBases(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category
    ) {
        return Result.success(
                knowledgeService.listKnowledgeBases(new KnowledgeBaseQueryDTO(keyword, status, category))
        );
    }
}
