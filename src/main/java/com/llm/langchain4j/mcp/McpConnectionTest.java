package com.llm.langchain4j.mcp;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.util.List;

/**
 * Quick test: connect to open-meteo-mcp-lite MCP server and call a tool.
 *
 * Run: mvn compile exec:java -Dexec.mainClass="com.llm.langchain4j.mcp.McpConnectionTest"
 */
public class McpConnectionTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== MCP 连接测试: open-meteo-mcp-lite ===\n");

        // 1. Start MCP server via npx + stdio
        System.out.println("[1] 启动 MCP 服务器 (npx -y open-meteo-mcp-lite)...");
        StdioMcpTransport transport = new StdioMcpTransport.Builder()
                .command(List.of("npx", "-y", "open-meteo-mcp-lite"))
                .logEvents(false)
                .build();

        // 2. Create MCP client
        System.out.println("[2] 创建 MCP 客户端...");
        McpClient client = new DefaultMcpClient.Builder()
                .transport(transport)
                .clientName("mcp-test")
                .clientVersion("1.0.0")
                .build();

        // 3. List tools
        System.out.println("[3] 获取工具列表...");
        List<ToolSpecification> tools = client.listTools();
        System.out.println("发现 " + tools.size() + " 个工具:");
        for (ToolSpecification t : tools) {
            System.out.println("  - " + t.name() + ": " + t.description());
        }

        // 4. Call set_default_location
        System.out.println("\n[4] 设置默认位置: Beijing");
        ToolExecutionRequest req1 = ToolExecutionRequest.builder()
                .name("set_default_location")
                .arguments("{\"location\": \"Beijing\"}")
                .build();
        System.out.println("结果: " + client.executeTool(req1));

        // 5. Call get_weather
        System.out.println("\n[5] 查询天气...");
        ToolExecutionRequest req2 = ToolExecutionRequest.builder()
                .name("get_weather")
                .arguments("{\"location\": \"Beijing\"}")
                .build();
        String weather = client.executeTool(req2);
        System.out.println("结果:\n" + weather);

        // 6. Close
        client.close();
        System.out.println("\n=== 测试完成 ===");
    }
}
