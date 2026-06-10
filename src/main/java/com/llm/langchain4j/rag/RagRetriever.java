package com.llm.langchain4j.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

public class RagRetriever {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final int maxResults;

    public RagRetriever(EmbeddingModel embeddingModel, int maxResults) {
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        this.embeddingModel = embeddingModel;
        this.maxResults = maxResults;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    public String search(String query) {
        if (embeddingModel == null) {
            return "知识库未初始化（Embedding 模型不可用）";
        }
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .build();
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
            List<EmbeddingMatch<TextSegment>> matches = result.matches();
            if (matches.isEmpty()) {
                return "未找到相关知识";
            }
            StringBuilder sb = new StringBuilder("知识库检索结果:\n\n");
            for (int i = 0; i < matches.size(); i++) {
                EmbeddingMatch<TextSegment> match = matches.get(i);
                sb.append("【片段 ").append(i + 1).append("】相似度: ")
                        .append(String.format("%.2f", match.score()))
                        .append("\n").append(match.embedded().text()).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "RAG 检索失败: " + e.getMessage();
        }
    }
}