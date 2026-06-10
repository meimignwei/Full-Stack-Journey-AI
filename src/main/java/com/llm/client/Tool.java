package com.llm.client;

/**
 * 工具接口 —— 每个工具提供名称、描述、参数 schema、执行方法，
 * 供 LLM function calling 使用
 */
public interface Tool {

    /** 工具名称，LLM 通过此名称决定调用哪个工具 */
    String getName();

    /** 工具功能描述，用于 LLM 理解何时该调用此工具 */
    String getDescription();

    /** 参数 JSON Schema，描述该工具需要的参数格式 */
    String getParametersJsonSchema();

    /**
     * 执行工具，接收 JSON 格式的参数，返回 JSON 格式的结果字符串
     * @param argumentsJson LLM 传来的参数 JSON，如 {"query": "今天天气"}
     * @return 工具执行结果的字符串
     */
    String execute(String argumentsJson);
}