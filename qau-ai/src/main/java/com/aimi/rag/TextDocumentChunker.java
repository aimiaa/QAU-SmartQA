package com.aimi.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 文本切片器：面向中文文档的“语义优先”分块。
 * 先按标题/段落切成语义块，再把相邻小块合并到目标长度，超长块用带重叠的滑窗兜底，
 * 块间保留 overlap 以延续上下文，尽量避免把句子或条款从中间切断。
 */
@Component
public class TextDocumentChunker {

    /** 标题行：Markdown #、“第X章/节/条”、“一、/1./（一）”等常见中文条目开头。 */
    private static final Pattern HEADING = Pattern.compile(
            "^\\s*(#{1,6}\\s|第[一二三四五六七八九十百千0-9]+[章节条款篇]|[一二三四五六七八九十]+[、.．]|\\d+[、.．\\s]|（[一二三四五六七八九十]+）|\\([一二三四五六七八九十]+\\))");

    @Value("${app.knowledge.chunk.size:800}")
    private int chunkSize;

    @Value("${app.knowledge.chunk.overlap:100}")
    private int overlap;

    public List<String> chunk(String rawText) {
        String text = normalize(rawText);
        List<String> chunks = new ArrayList<>();
        if (text.isEmpty()) {
            return chunks;
        }

        int size = Math.max(chunkSize, 100);
        int ov = Math.min(Math.max(overlap, 0), size / 2);

        StringBuilder buffer = new StringBuilder();
        for (String block : splitIntoSemanticBlocks(text)) {
            // 超长语义块：先落盘已缓冲内容，再对大块做带重叠滑窗
            if (block.length() > size) {
                flush(buffer, chunks);
                chunks.addAll(windowSplit(block, size, ov));
                continue;
            }
            // 合并会超目标长度：先落盘，再从上一片段尾部取 overlap 作为前缀延续上下文
            if (buffer.length() > 0 && buffer.length() + block.length() + 1 > size) {
                flush(buffer, chunks);
                String carry = tail(chunks.get(chunks.size() - 1), ov);
                if (!carry.isEmpty()) {
                    buffer.append(carry).append('\n');
                }
            }
            if (buffer.length() > 0) {
                buffer.append('\n');
            }
            buffer.append(block);
        }
        flush(buffer, chunks);
        return chunks;
    }

    /** 按标题与空行把正文切成语义块（段落/小节），保持原始顺序。 */
    private List<String> splitIntoSemanticBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                if (current.length() > 0) {
                    blocks.add(current.toString().strip());
                    current.setLength(0);
                }
                continue;
            }
            boolean heading = HEADING.matcher(trimmed).find();
            if (heading && current.length() > 0) {
                blocks.add(current.toString().strip());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(trimmed);
        }
        if (current.length() > 0) {
            blocks.add(current.toString().strip());
        }
        return blocks;
    }

    /** 超长块用定长滑窗 + 重叠切分，保证不丢内容。 */
    private List<String> windowSplit(String block, int size, int ov) {
        List<String> pieces = new ArrayList<>();
        int step = Math.max(1, size - ov);
        for (int start = 0; start < block.length(); start += step) {
            int end = Math.min(start + size, block.length());
            String piece = block.substring(start, end).strip();
            if (!piece.isEmpty()) {
                pieces.add(piece);
            }
            if (end == block.length()) {
                break;
            }
        }
        return pieces;
    }

    private void flush(StringBuilder buffer, List<String> chunks) {
        if (buffer.length() > 0) {
            String piece = buffer.toString().strip();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            buffer.setLength(0);
        }
    }

    /** 取上一片段末尾若干字符作为重叠前缀，延续跨块上下文。 */
    private String tail(String chunk, int ov) {
        if (ov <= 0 || chunk == null || chunk.isEmpty()) {
            return "";
        }
        return chunk.length() <= ov ? chunk : chunk.substring(chunk.length() - ov);
    }

    private String normalize(String rawText) {
        if (rawText == null) {
            return "";
        }
        return rawText.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }
}