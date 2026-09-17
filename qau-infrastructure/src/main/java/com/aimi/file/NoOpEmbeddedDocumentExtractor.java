package com.aimi.file;

import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;

/**
 * No-op embedded document extractor used to keep images and attachments out of
 * the main text stream, avoiding noisy embedded references and temporary paths.
 * <p>
 * The method signature matches Tika 2.9.2. Tika 3.x changes the fourth
 * argument to ParseContext, so this adapter must be updated with that upgrade.
 */
@Slf4j
public class NoOpEmbeddedDocumentExtractor implements EmbeddedDocumentExtractor {

    /**
     * 是否应该解析嵌入文档。
     *
     * @param metadata 文档元数据
     * @return 始终返回 false，禁用嵌入文档解析
     */
    @Override
    public boolean shouldParseEmbedded(Metadata metadata) {
        // 记录跳过的嵌入文档（使用字符串常量，兼容不同 Tika 版本）
        String resourceName = metadata.get("resourceName");
        if (resourceName != null) {
            log.debug("Skip embedded document: {}", resourceName);
        }
        return false;
    }

    /**
     * 解析嵌入文档（空实现）。
     * 由于 shouldParseEmbedded 恒为 false，此方法不会被调用。
     */
    @Override
    public void parseEmbedded(
            InputStream stream,
            ContentHandler handler,
            Metadata metadata,
            boolean outputHtml) {
        // 空实现，不执行任何操作
    }
}
