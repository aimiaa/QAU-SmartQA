package com.aimi.file;

import org.springframework.stereotype.Component;

/**
 * Normalizes extracted document text before chunking and embedding by removing
 * control-character noise, standardizing line breaks, and compressing whitespace.
 */
@Component
public class TextCleaningService {

    public String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String cleaned = text.replace("\r\n", "\n").replace('\r', '\n');
        // 剔除除 \t \n 以外的控制字符（PDF 抽取常见 \u0000、\u001A 等噪声）
        cleaned = cleaned.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]", "");
        // 连续空格/制表符压缩为一个空格，并去掉行尾空白
        cleaned = cleaned.replaceAll("[ \\t]{2,}", " ");
        cleaned = cleaned.replaceAll("(?m)[ \\t]+$", "");
        // 三个以上连续换行压缩为一个空行
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        return cleaned.strip();
    }
}
