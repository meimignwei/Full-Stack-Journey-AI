package com.llm.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 零框架依赖的大模型 API 客户端
 * 支持：function calling（工具调用）、对话记忆（滑动窗口）、流式输出、可视化追踪
 * 仅使用 JDK 11+ 自带的 java.net.http，不依赖任何第三方库
 */
public class LlmClient {

    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    private ToolRegistry toolRegistry;
    private ChatMemory chatMemory;
    private ToolCallListener toolCallListener;
    private boolean debug;
    private boolean trace = true;
    private int maxToolCallRounds = 5;

    public LlmClient(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ==================== 配置 ====================

    public LlmClient withTools(ToolRegistry registry) {
        this.toolRegistry = registry;
        return this;
    }

    public LlmClient withMemory(ChatMemory memory) {
        this.chatMemory = memory;
        return this;
    }

    public LlmClient withMaxToolCallRounds(int rounds) {
        this.maxToolCallRounds = rounds;
        return this;
    }

    public LlmClient withToolCallListener(ToolCallListener listener) {
        this.toolCallListener = listener;
        return this;
    }

    public LlmClient withDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public LlmClient withTrace(boolean trace) {
        this.trace = trace;
        return this;
    }

    public boolean isDebug() { return debug; }
    public boolean isTrace() { return trace; }
    public ToolRegistry getToolRegistry() { return toolRegistry; }
    public ChatMemory getChatMemory() { return chatMemory; }

    // ==================== 高层 API（记忆 + 工具） ====================

    /**
     * 发送消息，自动使用 ChatMemory 和 ToolRegistry（若已设置）
     */
    public String sendMessage(String userMessage) {
        // ── 追踪：入口 ──
        if (trace) {
            System.out.println();
            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║  用户输入: " + truncate(userMessage, 36));
            System.out.println("╚══════════════════════════════════════════╝");
        }

        if (chatMemory != null) {
            chatMemory.addUser(userMessage);
            if (trace) {
                System.out.println("  📝 记忆写入: user → " + truncate(userMessage, 50));
                System.out.println("  📊 记忆窗口: " + chatMemory.currentRounds() + " / " + chatMemory.getMaxRounds() + " 轮");
            }
        }

        List<Message> messages;
        if (chatMemory != null) {
            messages = new ArrayList<>(chatMemory.getContextMessages());
        } else {
            messages = new ArrayList<>();
            messages.add(new Message("user", userMessage));
        }

        String reply;
        if (toolRegistry != null && toolRegistry.size() > 0) {
            reply = chatWithToolsInternal(messages);
        } else {
            reply = chat(messages);
        }

        if (chatMemory != null) {
            chatMemory.addAssistant(reply);
            if (trace) {
                System.out.println("  📝 记忆写入: assistant → " + truncate(reply, 50));
                System.out.println("  📊 记忆窗口: " + chatMemory.currentRounds() + " / " + chatMemory.getMaxRounds() + " 轮");
            }
        }

        if (trace) {
            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║  AI 回复: " + truncate(reply, 36));
            System.out.println("╚══════════════════════════════════════════╝");
        }
        return reply;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "(null)";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ==================== 非流式调用 ====================

    public String chat(String userMessage) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", userMessage));
        return chat(messages);
    }

    public String chat(List<Message> messages) {
        String requestBody = buildRequestBody(messages, false, hasToolRegistry());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new LlmException("API 返回错误，状态码: " + response.statusCode() + ", 响应: " + response.body());
            }
            return extractContent(response.body());
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("请求失败: " + e.getMessage(), e);
        }
    }

    // ==================== Function Calling ====================

    /**
     * 带工具调用的对话循环 —— 核心方法
     *
     * 流程:
     *   1. 构建请求（messages + tools 定义） → 发送给 LLM
     *   2. LLM 判断：需要工具？ → 返回 tool_calls → 执行工具 → 追加结果 → 回到步骤 1
     *   3. LLM 判断：不需要工具？ → 返回文本内容 → 结束
     */
    private String chatWithToolsInternal(List<Message> messages) {
        int round = 0;
        while (round < maxToolCallRounds) {
            round++;

            // ── 追踪：请求 ──
            if (trace) {
                traceRequest(round, messages);
            }
            String requestBody = buildRequestBody(messages, false, true);
            if (debug) {
                System.err.println("[DEBUG] 请求 JSON:\n" + requestBody + "\n");
            }

            String responseBody;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/v1/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(Duration.ofMinutes(2))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new LlmException("API 返回错误，状态码: " + response.statusCode() + ", 响应: " + response.body());
                }
                responseBody = response.body();
            } catch (LlmException e) {
                throw e;
            } catch (Exception e) {
                throw new LlmException("请求失败: " + e.getMessage(), e);
            }

            if (debug) {
                System.err.println("[DEBUG] 响应 JSON:\n" + responseBody + "\n");
            }

            // 检查是否有 tool_calls
            List<ToolCall> toolCalls = extractToolCalls(responseBody);
            if (!toolCalls.isEmpty()) {
                // ── 追踪：LLM 决定调用工具 ──
                if (trace) {
                    traceToolCalls(toolCalls);
                }

                Message assistantMsg = new Message("assistant", null);
                assistantMsg.toolCalls = toolCalls;
                messages.add(assistantMsg);
                if (chatMemory != null) {
                    chatMemory.add(assistantMsg);
                    if (trace) {
                        System.out.println("     📝 记忆写入: assistant(tool_calls) → " + toolCalls.get(0).functionName);
                    }
                }

                for (ToolCall tc : toolCalls) {
                    Tool tool = toolRegistry.get(tc.functionName);
                    String result;
                    if (tool != null) {
                        result = tool.execute(tc.arguments);
                    } else {
                        result = "错误：未找到工具 " + tc.functionName;
                    }

                    if (toolCallListener != null) {
                        toolCallListener.onToolCall(tc.functionName, tc.arguments, result);
                    }

                    // ── 追踪：工具结果 ──
                    if (trace) {
                        traceToolResult(tc.functionName, result);
                    }

                    Message toolMsg = new Message("tool", result);
                    toolMsg.toolCallId = tc.id;
                    toolMsg.functionName = tc.functionName;
                    messages.add(toolMsg);

                    if (chatMemory != null) {
                        chatMemory.add(toolMsg);
                        if (trace) {
                            String shortResult = result.length() > 40 ? result.substring(0, 40) + "…" : result;
                            System.out.println("     📝 记忆写入: tool(" + tc.functionName + ") → " + shortResult);
                        }
                    }
                }
                if (trace && chatMemory != null) {
                    System.out.println("     📊 记忆窗口: " + chatMemory.currentRounds() + " / " + chatMemory.getMaxRounds() + " 轮");
                }
                continue;
            }

            // 没有 tool_calls，返回文本
            String content = extractContent(responseBody);
            if (trace) {
                traceFinalResponse(content);
            }
            return content;
        }
        throw new LlmException("Function calling 达到最大循环次数 " + maxToolCallRounds + "，仍未获得最终回复");
    }

    // ==================== 可视化追踪 ====================

    private void traceRequest(int round, List<Message> messages) {
        System.out.println();
        System.out.println("  ┌─ 第 " + round + " 轮: 发送 " + messages.size() + " 条消息到 LLM ─");
        for (Message m : messages) {
            String icon = roleIcon(m.role);
            String detail = "";
            if (m.toolCalls != null && !m.toolCalls.isEmpty()) {
                detail = " → 调用 " + m.toolCalls.get(0).functionName;
            }
            if (m.toolCallId != null) {
                detail = " → 返回 " + m.functionName + " 结果";
            }
            String body = m.content;
            if (body == null) body = "(tool_calls — 无文本)";
            else if (body.length() > 55) body = body.substring(0, 55) + "…";
            System.out.println("  │ " + icon + " [" + m.role + detail + "] " + body);
        }
        if (toolRegistry != null && toolRegistry.size() > 0) {
            System.out.println("  │ 🧰 可用工具: " + String.join(", ", toolRegistry.getToolNames()));
        }
        System.out.println("  └──────────────────────────────────────────");
    }

    private void traceToolCalls(List<ToolCall> toolCalls) {
        for (ToolCall tc : toolCalls) {
            System.out.println("  ╭─ 🔧 LLM 调用工具: " + tc.functionName);
            System.out.println("  │   参数: " + tc.arguments);
            System.out.println("  ╰──────────────────────────");
        }
    }

    private void traceToolResult(String toolName, String result) {
        String preview = result.length() > 70 ? result.substring(0, 70) + "…" : result;
        System.out.println("     ✔ " + toolName + " 执行完毕 → " + preview);
    }

    private void traceFinalResponse(String content) {
        System.out.println("     ✅ LLM 直接回复（无需再调用工具）");
    }

    private static String roleIcon(String role) {
        switch (role) {
            case "system":    return "⚙️";
            case "user":      return "👤";
            case "assistant": return "🤖";
            case "tool":      return "🔧";
            default:          return "  ";
        }
    }

    // ==================== 流式调用 (SSE) ====================

    public void chatStream(String userMessage, StreamListener listener) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", userMessage));
        chatStream(messages, listener);
    }

    public void chatStream(List<Message> messages, StreamListener listener) {
        String requestBody = buildRequestBody(messages, true, hasToolRegistry());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        listener.onError(new LlmException("API 返回错误，状态码: " + response.statusCode()));
                        return;
                    }
                    response.body().forEach(line -> {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) {
                                listener.onComplete();
                                return;
                            }
                            String content = extractDeltaContent(data);
                            if (content != null && !content.isEmpty()) {
                                listener.onToken(content);
                            }
                        }
                    });
                })
                .exceptionally(e -> {
                    listener.onError(new LlmException("流式请求失败: " + e.getMessage(), e));
                    return null;
                });
    }

    // ==================== JSON 构造 ====================

    private String buildRequestBody(List<Message> messages, boolean stream, boolean includeTools) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(escapeJson(model)).append("\",");
        sb.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            appendMessageJson(sb, messages.get(i));
        }
        sb.append("],");
        sb.append("\"stream\":").append(stream);
        if (includeTools && toolRegistry != null && toolRegistry.size() > 0) {
            sb.append(",\"tools\":").append(toolRegistry.toFunctionsJson());
        }
        sb.append("}");
        return sb.toString();
    }

    private void appendMessageJson(StringBuilder sb, Message m) {
        sb.append("{\"role\":\"").append(escapeJson(m.role)).append("\"");
        if (m.content != null) {
            sb.append(",\"content\":\"").append(escapeJson(m.content)).append("\"");
        }
        if (m.toolCallId != null) {
            sb.append(",\"tool_call_id\":\"").append(escapeJson(m.toolCallId)).append("\"");
        }
        if (m.functionName != null) {
            sb.append(",\"name\":\"").append(escapeJson(m.functionName)).append("\"");
        }
        if (m.toolCalls != null && !m.toolCalls.isEmpty()) {
            sb.append(",\"tool_calls\":[");
            for (int i = 0; i < m.toolCalls.size(); i++) {
                if (i > 0) sb.append(",");
                ToolCall tc = m.toolCalls.get(i);
                sb.append("{\"id\":\"").append(escapeJson(tc.id)).append("\",");
                sb.append("\"type\":\"function\",");
                sb.append("\"function\":{\"name\":\"").append(escapeJson(tc.functionName))
                        .append("\",\"arguments\":\"").append(escapeJson(tc.arguments)).append("\"}");
                sb.append("}");
            }
            sb.append("]");
        }
        sb.append("}");
    }

    // ==================== 响应解析 ====================

    private String extractContent(String responseBody) {
        String key = "\"content\":\"";
        int start = responseBody.indexOf(key);
        if (start == -1) {
            if (responseBody.contains("\"tool_calls\"")) {
                return "";
            }
            throw new LlmException("无法解析响应中的 content: " + responseBody);
        }
        start += key.length();
        return parseJsonString(responseBody, start);
    }

    private String extractDeltaContent(String sseData) {
        String key = "\"content\":\"";
        int start = sseData.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        return parseJsonString(sseData, start);
    }

    private List<ToolCall> extractToolCalls(String responseBody) {
        List<ToolCall> result = new ArrayList<>();
        int arrStart = responseBody.indexOf("\"tool_calls\":[");
        if (arrStart == -1) return result;

        int pos = arrStart + "\"tool_calls\":[".length();
        while (pos < responseBody.length()) {
            char c = responseBody.charAt(pos);
            if (c == ']') break;
            if (c == '{') {
                String id = null;
                String fnName = null;
                String fnArgs = null;

                int idIdx = responseBody.indexOf("\"id\":\"", pos);
                if (idIdx != -1 && idIdx < responseBody.indexOf('}', pos)) {
                    idIdx += "\"id\":\"".length();
                    id = parseJsonString(responseBody, idIdx);
                }

                int nameIdx = responseBody.indexOf("\"name\":\"", pos);
                if (nameIdx != -1 && nameIdx < responseBody.indexOf('}', pos)) {
                    nameIdx += "\"name\":\"".length();
                    fnName = parseJsonString(responseBody, nameIdx);
                }

                int argsIdx = responseBody.indexOf("\"arguments\":\"", pos);
                if (argsIdx != -1 && argsIdx < responseBody.indexOf('}', pos)) {
                    argsIdx += "\"arguments\":\"".length();
                    fnArgs = parseJsonString(responseBody, argsIdx);
                }

                if (id != null && fnName != null) {
                    result.add(new ToolCall(id, fnName, fnArgs != null ? fnArgs : "{}"));
                }

                int close = responseBody.indexOf('}', pos);
                if (close != -1) {
                    pos = close + 1;
                } else {
                    pos++;
                }
            } else {
                pos++;
            }
        }
        return result;
    }

    // ==================== JSON 工具方法 ====================

    private String parseJsonString(String json, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"');  i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case '/':  sb.append('/');  i++; break;
                    case 'n':  sb.append('\n'); i++; break;
                    case 'r':  sb.append('\r'); i++; break;
                    case 't':  sb.append('\t'); i++; break;
                    case 'u':
                        if (i + 5 < json.length()) {
                            String hex = json.substring(i + 2, i + 6);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 5;
                        }
                        break;
                    default:
                        sb.append(c);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
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

    private boolean hasToolRegistry() {
        return toolRegistry != null && toolRegistry.size() > 0;
    }

    // ==================== 数据类 ====================

    public static class Message {
        public final String role;
        public final String content;
        public List<ToolCall> toolCalls;
        public String toolCallId;
        public String functionName;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class ToolCall {
        public final String id;
        public final String functionName;
        public final String arguments;

        public ToolCall(String id, String functionName, String arguments) {
            this.id = id;
            this.functionName = functionName;
            this.arguments = arguments;
        }

        @Override
        public String toString() {
            return "ToolCall{id='" + id + "', function='" + functionName + "', args=" + arguments + "}";
        }
    }

    public static class LlmException extends RuntimeException {
        public LlmException(String message) { super(message); }
        public LlmException(String message, Throwable cause) { super(message, cause); }
    }

    public interface StreamListener {
        void onToken(String token);
        default void onComplete() {}
        default void onError(Throwable e) { e.printStackTrace(); }
    }

    /** 工具调用监听器 —— 每当 LLM 调用一个工具时触发 */
    public interface ToolCallListener {
        void onToolCall(String toolName, String arguments, String result);
    }
}