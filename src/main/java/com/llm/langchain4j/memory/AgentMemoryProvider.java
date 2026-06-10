package com.llm.langchain4j.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentMemoryProvider implements ChatMemoryProvider {

    private final Map<Object, ChatMemory> memories = new ConcurrentHashMap<>();
    private final int maxMessages;

    public AgentMemoryProvider(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    @Override
    public ChatMemory get(Object memoryId) {
        return memories.computeIfAbsent(memoryId,
                id -> MessageWindowChatMemory.withMaxMessages(maxMessages));
    }

    public ChatMemory getMemory(Object memoryId) {
        return memories.get(memoryId);
    }

    public void clear(Object memoryId) {
        ChatMemory memory = memories.get(memoryId);
        if (memory != null) {
            memory.clear();
        }
    }

    public int getMaxMessages() {
        return maxMessages;
    }
}