package com.llm.langchain4j.mcp;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;

import java.util.ArrayList;
import java.util.List;

public class McpToolProvider implements AutoCloseable {

    private final List<ToolSpecification> mcpTools = new ArrayList<>();
    private final List<Object> mcpToolObjects = new ArrayList<>();
    private boolean connected = false;

    public boolean connectDemoServer() {
        try {
            // In a full implementation, this would launch DemoMcpServer as subprocess
            // via StdioMcpTransport and connect using LangChain4j McpClient
            System.out.println("[MCP] 尝试连接 Demo Server...");
            registerFallbackTools();
            System.out.println("[MCP] 使用降级内置模式（MCP SDK 未集成）");
            connected = true;
            return true;
        } catch (Exception e) {
            System.err.println("[MCP] 连接失败: " + e.getMessage());
            registerFallbackTools();
            return false;
        }
    }

    private void registerFallbackTools() {
        mcpToolObjects.add(new McpFallbackTools());

        mcpTools.add(ToolSpecification.builder()
                .name("get_weather")
                .description("获取指定城市的天气信息 [MCP]")
                .build());
        mcpTools.add(ToolSpecification.builder()
                .name("read_local_file")
                .description("读取本地文件内容 [MCP]")
                .build());

        System.out.println("[MCP] 已注册 " + mcpTools.size() + " 个 MCP 工具（降级模式）:");
        for (ToolSpecification t : mcpTools) {
            System.out.println("  - [MCP] " + t.name() + ": " + t.description());
        }
    }

    public List<ToolSpecification> getToolSpecifications() { return mcpTools; }
    public List<Object> getToolObjects() { return mcpToolObjects; }
    public boolean isConnected() { return connected; }

    @Override
    public void close() {}

    public static class McpFallbackTools {

        @Tool("获取指定城市的天气信息 [MCP]")
        public String get_weather(String city) {
            return String.format("%s 天气:\n☀ 晴 | 温度: 25°C | 湿度: 60%% | 风速: 3m/s", city);
        }

        @Tool("读取本地文件内容 [MCP]")
        public String read_local_file(String path) {
            try {
                return java.nio.file.Files.readString(java.nio.file.Path.of(path));
            } catch (Exception e) {
                return "读取失败: " + e.getMessage();
            }
        }
    }
}
