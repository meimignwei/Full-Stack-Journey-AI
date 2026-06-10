package com.llm.langchain4j.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

import java.nio.file.Path;
import java.util.List;

public class KnowledgeBaseInitializer {

    private final String knowledgeDir;
    private final RagRetriever retriever;

    public KnowledgeBaseInitializer(String knowledgeDir, RagRetriever retriever) {
        this.knowledgeDir = knowledgeDir;
        this.retriever = retriever;
    }

    public int initialize() {
        Path dirPath = Path.of(knowledgeDir);
        if (!dirPath.toFile().exists() || !dirPath.toFile().isDirectory()) {
            System.out.println("[RAG] 知识库目录不存在，跳过初始化: " + knowledgeDir);
            return 0;
        }

        List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                dirPath,
                new TextDocumentParser()
        );

        if (documents.isEmpty()) {
            System.out.println("[RAG] 知识库目录中没有文档");
            return 0;
        }

        EmbeddingStore<TextSegment> store = retriever.getEmbeddingStore();
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 50))
                .embeddingStore(store)
                .build();

        ingestor.ingest(documents);

        System.out.println("[RAG] 已加载 " + documents.size() + " 个文档到知识库");
        return documents.size();
    }
}