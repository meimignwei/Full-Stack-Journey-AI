package com.llm.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 工具注册中心 —— 维护 工具名 → (描述, 方法) 的映射
 * 负责生成 OpenAI function calling 格式的 tools 数组 JSON
 */
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    /** 注册一个工具 */
    public ToolRegistry register(Tool tool) {
        tools.put(tool.getName(), tool);
        return this; // 支持链式调用
    }

    /** 按名称查找工具 */
    public Tool get(String name) {
        return tools.get(name);
    }

    /** 所有已注册的工具名称 */
    public Set<String> getToolNames() {
        return tools.keySet();
    }

    /** 已注册工具数量 */
    public int size() {
        return tools.size();
    }

    /**
     * 生成 OpenAI function calling 格式的 tools 数组 JSON
     * 格式: [{"type":"function","function":{"name":"...","description":"...","parameters":{...}}}]
     */
    public String toFunctionsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Tool tool : tools.values()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"type\":\"function\",\"function\":{");
            sb.append("\"name\":\"").append(escapeJson(tool.getName())).append("\",");
            sb.append("\"description\":\"").append(escapeJson(tool.getDescription())).append("\",");
            sb.append("\"parameters\":").append(tool.getParametersJsonSchema());
            sb.append("}}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }
}