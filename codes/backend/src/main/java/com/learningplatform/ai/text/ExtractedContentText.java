/* 文件职责：以不可变记录表示Extracted学习资料Text数据，并作为模块内部或接口层的数据契约。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：文本提取与规范化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.text;

import java.util.List;

/**
 * 以不可变记录表示Extracted学习资料Text数据，并作为模块内部或接口层的数据契约。
 *
 * <p>职责边界：遵守 AI 任务、对话、分析与供应商调用 模块的职责边界。</p>
 */
public record ExtractedContentText(
        Long contentId,
        String title,
        String text,
        String sourceVersion,
        List<String> includedTextFiles
) {
    public ExtractedContentText {
        includedTextFiles = List.copyOf(includedTextFiles);
    }
}
