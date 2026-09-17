package com.aimi.rag;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 文本切片器：按固定窗口 + 重叠把长文本切成适合向量检索的片段。
 * 中文场景下按字符切分即可，重叠能保留跨片段的上下文。
 */
@Component
public class TextDocumentChunker {

    @Value("${app.knowledge.chunk.size:500}")
    private int chunkSize;

    @Value("${app.knowledge.chunk.overlap:50}")
    private int overlap;

    public List<String> chunk(String rawText) {
        String text = normalize(rawText);
        List<String> chunks = new ArrayList<>();
        if (text.isEmpty()) {
            return chunks;
        }

        int size = Math.max(chunkSize, 50);
        int step = Math.max(1, size - Math.max(overlap, 0));

        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(start + size, text.length());
            chunks.add(text.substring(start, end));
            if (end == text.length()) {
                break;
            }
        }

        return chunks;
    }

    private String normalize(String rawText) {
        if (rawText == null) {
            return "";
        }
        // 合并多余空行、去掉每行首尾空白，让切片更紧凑
        return rawText.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }
}