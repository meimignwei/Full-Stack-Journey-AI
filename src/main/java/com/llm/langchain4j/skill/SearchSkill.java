package com.llm.langchain4j.skill;

import dev.langchain4j.agent.tool.Tool;

public class SearchSkill {

    @Tool("搜索互联网获取最新信息，输入搜索关键词，返回相关结果摘要")
    public String search(String query) {
        return String.format("""
                搜索结果: "%s"

                1. 【AI 最新动态】OpenAI 发布新一代多模态模型，支持文本、图像、音频的联合推理。
                2. 【技术进展】RAG 技术持续演进，Agentic RAG 成为新的研究方向。
                3. 【行业应用】多家企业已将 AI Agent 应用于客服、代码生成等场景。
                4. 【开源动态】LangChain4j 1.0 版本发布，MCP 支持更加完善。

                (模拟搜索，实际可接入 SerpAPI 或 Tavily)
                """, query);
    }
}