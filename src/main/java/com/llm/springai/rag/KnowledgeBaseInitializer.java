package com.llm.springai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class KnowledgeBaseInitializer {

    private final String knowledgeDir;
    private final RagRetriever retriever;
    private final EmbeddingModel embeddingModel;

    public KnowledgeBaseInitializer(RagRetriever retriever, EmbeddingModel embeddingModel) {
        this("knowledge", retriever, embeddingModel);
    }

    public KnowledgeBaseInitializer(String knowledgeDir, RagRetriever retriever, EmbeddingModel embeddingModel) {
        this.knowledgeDir = knowledgeDir;
        this.retriever = retriever;
        this.embeddingModel = embeddingModel;
    }

    public int initialize() {
        Path dirPath = Path.of(knowledgeDir);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            System.out.println("[RAG] 知识库目录不存在，跳过初始化: " + knowledgeDir);
            return 0;
        }

        List<Document> documents = new ArrayList<>();
        try (Stream<Path> files = Files.list(dirPath)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String content = Files.readString(file);
                    documents.add(new Document(content));
                } catch (IOException e) {
                    System.err.println("[RAG] 读取文件失败: " + file + " - " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.err.println("[RAG] 遍历知识库目录失败: " + e.getMessage());
            return 0;
        }

        if (documents.isEmpty()) {
            System.out.println("[RAG] 知识库目录中没有文档");
            return 0;
        }

        VectorStore store = retriever.getVectorStore();
        store.add(documents);

        System.out.println("[RAG] 已加载 " + documents.size() + " 个文档到知识库");
        return documents.size();
    }
}
