package com.llm.langchain4j.rag;

import dev.langchain4j.model.embedding.onnx.bgesmallzhv15q.BgeSmallZhV15QuantizedEmbeddingModel;

/**
 * 快速测试 RAG 管道：初始化知识库并执行查询。
 * 运行: mvn compile exec:java -Dexec.mainClass="com.llm.langchain4j.rag.RagTest"
 */
public class RagTest {

    public static void main(String[] args) {
        System.out.println("=== RAG 管道测试 ===\n");

        System.out.println("加载本地 Embedding 模型: BGE-small-zh-v1.5 (quantized)...");
        BgeSmallZhV15QuantizedEmbeddingModel embeddingModel = new BgeSmallZhV15QuantizedEmbeddingModel();
        System.out.println("Embedding 模型就绪, 维度: " + embeddingModel.dimension() + "\n");

        RagRetriever retriever = new RagRetriever(embeddingModel, 3);
        KnowledgeBaseInitializer initializer = new KnowledgeBaseInitializer("knowledge", retriever, embeddingModel);
        int docCount = initializer.initialize();
        System.out.println("已加载文档数: " + docCount + "\n");

        String[] queries = {
                "这个项目的架构是什么",
                "如何运行这个项目",
                "RAG 管道是如何工作的",
                "MCP 降级模式是什么",
                "有哪些设计决策"
        };

        for (String query : queries) {
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("查询: " + query);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            String result = retriever.search(query);
            System.out.println(result);
        }

        System.out.println("=== 测试完成 ===");
        System.exit(0);
    }
}
