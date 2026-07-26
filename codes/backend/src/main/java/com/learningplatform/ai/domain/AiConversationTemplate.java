package com.learningplatform.ai.domain;

public enum AiConversationTemplate {
    QUIZ_REINFORCEMENT(
            "出题巩固",
            """
            请根据当前学习资料生成一组用于复习巩固的题目。
            要求：
            1. 覆盖资料中的主要知识点，并按照由易到难的顺序组织；
            2. 共生成 6 道题，包含单选题、判断题和简答题；
            3. 先完整列出题目，再单独列出参考答案和解析，避免用户立即看到答案；
            4. 每道题都要注明考查的知识点；
            5. 只能依据当前资料出题，资料未提供的信息不得编造；
            6. 使用清晰的中文 Markdown 格式。
            """
    ),
    DIVERGENT_THINKING(
            "发散思维",
            """
            请围绕当前学习资料进行知识发散，并给出后续学习路线。
            要求：
            1. 先提炼资料中的核心知识点；
            2. 分别列出相关的前置知识、横向关联知识和进阶知识；
            3. 解释这些知识点与当前资料之间的关系；
            4. 给出一条由浅入深、可执行的后续学习顺序；
            5. 对资料未直接提供、但属于扩展建议的内容明确标注“扩展知识”，不要伪装成资料原文；
            6. 使用清晰的中文 Markdown 格式。
            """
    );

    private final String displayText;
    private final String prompt;

    AiConversationTemplate(String displayText, String prompt) {
        this.displayText = displayText;
        this.prompt = prompt;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getPrompt() {
        return prompt;
    }
}
