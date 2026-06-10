package com.llm.langchain4j.mcp;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;

import java.util.ArrayList;
import java.util.List;

public class McpToolProvider implements AutoCloseable {

    private McpClient mcpClient;
    private ToolProvider toolProvider;
    private final List<ToolSpecification> toolSpecs = new ArrayList<>();
    private final List<Object> fallbackToolObjects = new ArrayList<>();
    private boolean connected = false;
    private boolean realMcp = false;

    /**
     * Connect to a free MCP server (open-meteo-mcp-lite) for weather data.
     * Falls back to built-in tools if the MCP server is unavailable.
     */
    public boolean connectDemoServer() {
        try {
            System.out.println("[MCP] 正在连接 open-meteo-mcp-lite (免费天气 MCP)...");

            StdioMcpTransport transport = new StdioMcpTransport.Builder()
                    .command(List.of("npx", "-y", "open-meteo-mcp-lite"))
                    .logEvents(false)
                    .build();

            mcpClient = new DefaultMcpClient.Builder()
                    .transport(transport)
                    .clientName("gradual-evolution-agent")
                    .clientVersion("1.0.0")
                    .build();

            // Discover tools exposed by the MCP server
            toolSpecs.clear();
            toolSpecs.addAll(mcpClient.listTools());

            // Wrap in LangChain4j ToolProvider for AiServices integration
            toolProvider = dev.langchain4j.mcp.McpToolProvider.builder()
                    .mcpClients(mcpClient)
                    .failIfOneServerFails(false)
                    .build();

            realMcp = true;
            connected = true;
            System.out.println("[MCP] 已连接真实 MCP 服务器, 获取 " + toolSpecs.size() + " 个工具:");
            for (ToolSpecification t : toolSpecs) {
                System.out.println("  - [MCP] " + t.name() + ": " + t.description());
            }
            return true;

        } catch (Exception e) {
            System.err.println("[MCP] MCP 服务器连接失败: " + e.getMessage());
            System.err.println("[MCP] 降级为内置工具模式");
            registerFallbackTools();
            return false;
        }
    }

    private void registerFallbackTools() {
        realMcp = false;
        connected = true;
        McpFallbackTools fallbackTools = new McpFallbackTools();
        fallbackToolObjects.add(fallbackTools);

        toolSpecs.add(ToolSpecification.builder()
                .name("get_weather")
                .description("获取指定城市的天气信息 [MCP 降级]")
                .build());
        toolSpecs.add(ToolSpecification.builder()
                .name("read_local_file")
                .description("读取本地文件内容 [MCP 降级]")
                .build());

        System.out.println("[MCP] 已注册 " + toolSpecs.size() + " 个降级工具:");
        for (ToolSpecification t : toolSpecs) {
            System.out.println("  - [MCP] " + t.name() + ": " + t.description());
        }
    }

    /** Expose the ToolProvider for AiServices.builder().toolProvider() */
    public ToolProvider getToolProvider() {
        return toolProvider;
    }

    /** Expose fallback @Tool objects for AiServices.builder().tools() */
    public List<Object> getFallbackToolObjects() {
        return fallbackToolObjects;
    }

    public List<ToolSpecification> getToolSpecifications() {
        return toolSpecs;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isRealMcp() {
        return realMcp;
    }

    @Override
    public void close() {
        if (mcpClient != null) {
            try {
                mcpClient.close();
            } catch (Exception e) {
                System.err.println("[MCP] 关闭 MCP 客户端时出错: " + e.getMessage());
            }
        }
    }

    public static class McpFallbackTools {

        @Tool("获取指定城市的天气信息 [MCP 降级模式]")
        public String get_weather(String city) {
            return String.format("%s 天气:\n☀ 晴 | 温度: 25°C | 湿度: 60%% | 风速: 3m/s", city);
        }

        @Tool("读取本地文件内容 [MCP 降级模式]")
        public String read_local_file(String path) {
            try {
                return java.nio.file.Files.readString(java.nio.file.Path.of(path));
            } catch (Exception e) {
                return "读取失败: " + e.getMessage();
            }
        }
    }
}