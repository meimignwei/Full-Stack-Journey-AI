package com.llm.springai.tool;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@Component
public class SearchTool implements Function<SearchTool.Request, String> {

    public record Request(
            String query
    ) {}

    @Override
    public String apply(Request request) {
        String query = request.query;
        if (query == null || query.isBlank()) {
            return "错误：缺少搜索关键词";
        }
        return simulateSearch(query);
    }

    private String simulateSearch(String query) {
        String[] snippets = {
                "根据多个来源的综合信息，关于「" + query + "」的最新资料显示，该话题近期受到广泛关注。",
                "权威来源指出，「" + query + "」相关内容在 2026 年有显著进展，主要涉及技术创新和应用落地。",
                "社区讨论中，「" + query + "」被频繁提及，多数观点认为其发展前景乐观。",
        };
        String picked = snippets[ThreadLocalRandom.current().nextInt(snippets.length)];
        return "搜索结果（" + Instant.now() + "）：\n" + picked;
    }
}
