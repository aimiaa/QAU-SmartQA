package com.aimi.controller;

import com.aimi.dto.knowledge.KnowledgeBaseDTO;
import com.aimi.dto.knowledge.KnowledgeBaseQueryDTO;
import com.aimi.result.Result;
import com.aimi.service.KnowledgeDocumentService;
import com.aimi.service.KnowledgeService;
import com.aimi.vo.knowledge.KnowledgeBaseVO;
import com.aimi.vo.knowledge.KnowledgeDocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/knowledge-bases")
@Tag(name = "knowledge", description = "Knowledge base related operations")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeDocumentService knowledgeDocumentService;

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

    /**
     * 创建一个新的知识库。
     * @param dto 知识库DTO
     * @return 创建结果
     */
    @Operation(summary = "创建一个新的知识库")
    @PostMapping
    public Result<KnowledgeBaseVO> createKnowledgeBase(@RequestBody KnowledgeBaseDTO dto) {
        return Result.success(knowledgeService.createKnowledgeBase(dto));
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledgeBase(@PathVariable Long id) {
        knowledgeService.deleteKnowledgeBase(id);
        return Result.success();
    }

    @Operation(summary = "向指定知识库上传文档")
    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<KnowledgeDocumentVO> uploadDocument(@PathVariable Long id,
                                                      @RequestPart("file") MultipartFile file) {
        return Result.success(knowledgeDocumentService.uploadDocument(id, file));
    }

    @Operation(summary = "查询知识库文档")
    @GetMapping("/{id}/documents")
    public Result<List<KnowledgeDocumentVO>> listDocuments(@PathVariable Long id) {
        return Result.success(knowledgeDocumentService.listDocuments(id));
    }

    @Operation(summary = "删除知识库文档")
    @DeleteMapping("/{id}/documents/{documentId}")
    public Result<Void> deleteDocument(@PathVariable Long id, @PathVariable Long documentId) {
        knowledgeDocumentService.deleteDocument(id, documentId);
        return Result.success();
    }
}
