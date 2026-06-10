package com.llm.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AgentService {

    @SystemMessage("""
            你是一个AI 助手，具备以下能力：
            - 使用计算器进行数学计算
            - 使用搜索引擎获取最新信息
            - 查询当前时间和日期
            - 检索知识库获取专业知识
            - 调用 MCP 工具获取天气和文件信息

            回答简洁、准确。当你需要更多信息时，主动使用工具获取。
            """)
    String chat(@MemoryId String userId, @UserMessage String message);
}