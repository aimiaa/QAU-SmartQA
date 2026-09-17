package com.aimi.file;

import com.aimi.exception.BusinessException;
import com.aimi.exception.ErrorCode;
import com.aimi.storage.FileStorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

/**
 * Shared document parsing core for the infrastructure layer.
 * Apache Tika detects the real file format and extracts text from PDF, Word,
 * plain-text, and Markdown documents for reuse by knowledge-related modules.
 */
@Slf4j
@Service
public class DocumentParseService {

    /** Maximum extracted body size per document to prevent excessive memory use. */
    private static final int MAX_TEXT_LENGTH = 5 * 1024 * 1024;

    private final TextCleaningService textCleaningService;

    public DocumentParseService(TextCleaningService textCleaningService) {
        this.textCleaningService = textCleaningService;
    }

    /** Parse an uploaded document and return cleaned text. */
    public String parseContent(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        log.info("开始解析文件: {}", fileName);

        if (file.isEmpty() || file.getSize() == 0) {
            log.warn("文件为空: {}", fileName);
            return "";
        }

        try (InputStream inputStream = file.getInputStream()) {
            return doParse(inputStream, fileName);
        } catch (IOException | TikaException | SAXException e) {
            log.error("文件解析失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件解析失败: " + e.getMessage());
        }
    }

    /** Parse document bytes and return cleaned text. */
    public String parseContent(byte[] fileBytes, String fileName) {
        log.info("开始解析文件（从字节数组）: {}", fileName);

        if (fileBytes == null || fileBytes.length == 0) {
            log.warn("文件字节数组为空: {}", fileName);
            return "";
        }

        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            return doParse(inputStream, fileName);
        } catch (IOException | TikaException | SAXException e) {
            log.error("文件解析失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 兼容入口：既有调用方按 (字节数组, 扩展名) 调用。
     * fileType 仅作日志线索，真实格式由 Tika 按内容自动探测，不再依赖扩展名。
     */
    public String extract(byte[] content, String fileType) {
        return parseContent(content, fileType == null ? "unknown" : fileType);
    }

    /** Download a stored document and parse its content. */
    public String downloadAndParseContent(FileStorageService storageService, String storageUrl, String originalFilename) {
        try {
            byte[] fileBytes = storageService.retrieve(storageUrl);
            if (fileBytes == null || fileBytes.length == 0) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "下载文件失败");
            }
            return parseContent(fileBytes, originalFilename);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("下载并解析文件失败: storageUrl={}, error={}", storageUrl, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "下载并解析文件失败: " + e.getMessage());
        }
    }

    /**
     * Configure Tika explicitly so parsing stays bounded and predictable:
     * extract only the body, skip embedded resources, and improve PDF layout order.
     */
    private String doParse(InputStream inputStream, String fileName) throws IOException, TikaException, SAXException {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_LENGTH);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        context.set(Parser.class, parser);
        context.set(EmbeddedDocumentExtractor.class, new NoOpEmbeddedDocumentExtractor());

        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(false);
        pdfConfig.setSortByPosition(true);
        context.set(PDFParserConfig.class, pdfConfig);

        parser.parse(inputStream, handler, metadata, context);

        String cleanedContent = textCleaningService.cleanText(handler.toString());
        log.info("文件解析成功: {}, 提取文本长度 {} 字符", fileName, cleanedContent.length());
        return cleanedContent;
    }
}
