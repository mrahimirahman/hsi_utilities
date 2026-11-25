package com.hospital.maintenance.dto;

import java.util.List;

public class AssistantChatRequest {
    private String message;
    private List<AssistantChatMessage> history;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AssistantChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<AssistantChatMessage> history) {
        this.history = history;
    }
}
