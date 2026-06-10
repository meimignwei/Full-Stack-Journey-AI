package com.llm.client;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟搜索引擎工具 —— 用于演示 function calling
 * 实际项目可替换为真正的搜索 API 调用
 */
public class SearchTool implements Tool {

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getDescription() {
        return "搜索互联网获取最新信息。当需要查找实时数据、新闻、或你不知道的事实时使用此工具。";
    }

    @Override
    public String getParametersJsonSchema() {
        return "{" +
                "\"type\":\"object\"," +
                "\"properties\":{" +
                    "\"query\":{" +
                        "\"type\":\"string\"," +
                        "\"description\":\"搜索关键词或问题\"" +
                    "}" +
                "}," +
                "\"required\":[\"query\"]" +
                "}";
    }

    @Override
    public String execute(String argumentsJson) {
        String query = extractStringField(argumentsJson, "query");
        if (query == null || query.isBlank()) {
            return "错误：缺少搜索关键词";
        }
        // 模拟搜索结果（实际项目替换为 HTTP 调用搜索引擎 API）
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

    /** 从简单 JSON 中提取字符串字段（避免引入 JSON 库） */
    static String extractStringField(String json, String fieldName) {
        String key = "\"" + fieldName + "\":\"";
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        StringBuilder val = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"':  val.append('"');  i++; break;
                    case '\\': val.append('\\'); i++; break;
                    case 'n':  val.append('\n'); i++; break;
                    default:   val.append(c);
                }
            } else if (c == '"') {
                break;
            } else {
                val.append(c);
            }
        }
        return val.toString();
    }
}