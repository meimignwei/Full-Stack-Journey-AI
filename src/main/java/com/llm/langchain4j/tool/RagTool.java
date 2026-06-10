package com.llm.langchain4j.tool;

import com.llm.langchain4j.rag.RagRetriever;
import dev.langchain4j.agent.tool.Tool;

public class RagTool {

    private final RagRetriever retriever;

    public RagTool(RagRetriever retriever) {
        this.retriever = retriever;
    }

    @Tool("从本地知识库中检索相关文档，输入查询问题，返回匹配的文档片段")
    public String ragSearch(String question) {
        return retriever.search(question);
    }
}