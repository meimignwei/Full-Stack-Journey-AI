package com.llm.langchain4j.skill;

import com.llm.langchain4j.rag.RagRetriever;
import dev.langchain4j.agent.tool.Tool;

public class RagSkill {

    private final RagRetriever retriever;

    public RagSkill(RagRetriever retriever) {
        this.retriever = retriever;
    }

    @Tool("从本地知识库中检索相关文档，输入查询问题，返回匹配的文档片段")
    public String ragSearch(String question) {
        return retriever.search(question);
    }
}