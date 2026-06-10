package com.llm.langchain4j.mcp;

/**
 * MCP Demo Server placeholder.
 * To enable real MCP, add io.modelcontextprotocol.sdk:mcp dependency and implement
 * a stdio-based MCP server with get_weather and read_local_file tools.
 */
public class DemoMcpServer {

    public static void main(String[] args) {
        System.err.println("[MCP Demo Server] Starting on stdio...");
        System.err.println("[MCP Demo Server] To enable: add io.modelcontextprotocol.sdk:mcp dependency");
        System.err.println("[MCP Demo Server] Tools available: get_weather, read_local_file");
        // In a real implementation, this would use McpServer.sync() with StdioServerTransport
        System.err.println("[MCP Demo Server] Ready (simulated)");
    }
}