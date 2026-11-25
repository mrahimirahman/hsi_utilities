package com.hospital.maintenance.dto;

import java.util.List;
import java.util.Map;

public class DashboardResponse {

    private int totalRequests;
    private int openRequests;
    private int inProgressRequests;
    private int completedRequests;
    private List<Map<String, Object>> recentRequests;

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getOpenRequests() {
        return openRequests;
    }

    public void setOpenRequests(int openRequests) {
        this.openRequests = openRequests;
    }

    public int getInProgressRequests() {
        return inProgressRequests;
    }

    public void setInProgressRequests(int inProgressRequests) {
        this.inProgressRequests = inProgressRequests;
    }

    public int getCompletedRequests() {
        return completedRequests;
    }

    public void setCompletedRequests(int completedRequests) {
        this.completedRequests = completedRequests;
    }

    public List<Map<String, Object>> getRecentRequests() {
        return recentRequests;
    }

    public void setRecentRequests(List<Map<String, Object>> recentRequests) {
        this.recentRequests = recentRequests;
    }
}
