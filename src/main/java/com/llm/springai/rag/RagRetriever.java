package com.llm.springai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagRetriever {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final int maxResults;

    public RagRetriever(EmbeddingModel embeddingModel, int maxResults) {
        this.embeddingModel = embeddingModel;
        this.maxResults = maxResults;
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
    }

    public VectorStore getVectorStore() {
        return vectorStore;
    }

    public String search(String query) {
        if (embeddingModel == null) {
            return "知识库未初始化（Embedding 模型不可用）";
        }
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(maxResults)
                    .build();
            List<Document> results = vectorStore.similaritySearch(request);
            if (results.isEmpty()) {
                return "未找到相关知识";
            }
            StringBuilder sb = new StringBuilder("知识库检索结果:\n\n");
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                sb.append("【片段 ").append(i + 1).append("】")
                        .append("\n").append(doc.getText()).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "RAG 检索失败: " + e.getMessage();
        }
    }
}
