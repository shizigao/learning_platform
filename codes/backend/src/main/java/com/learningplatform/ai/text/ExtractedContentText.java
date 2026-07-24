package com.learningplatform.ai.text;

import java.util.List;

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
