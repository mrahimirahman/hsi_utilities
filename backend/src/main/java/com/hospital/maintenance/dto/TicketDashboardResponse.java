package com.hospital.maintenance.dto;

import java.util.List;

public class TicketDashboardResponse {

    private String fromDate;
    private String toDate;
    private int totalTickets;
    private int totalTransactions;
    private int dischargeCount;
    private int deathCount;
    private int pd103Count;
    private int activeUsers;
    private List<LabelCount> daily;
    private List<LabelCount> topUsers;
    private List<LabelCount> fixModules;

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public int getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(int totalTickets) {
        this.totalTickets = totalTickets;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public int getDischargeCount() {
        return dischargeCount;
    }

    public void setDischargeCount(int dischargeCount) {
        this.dischargeCount = dischargeCount;
    }

    public int getDeathCount() {
        return deathCount;
    }

    public void setDeathCount(int deathCount) {
        this.deathCount = deathCount;
    }

    public int getPd103Count() {
        return pd103Count;
    }

    public void setPd103Count(int pd103Count) {
        this.pd103Count = pd103Count;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public List<LabelCount> getDaily() {
        return daily;
    }

    public void setDaily(List<LabelCount> daily) {
        this.daily = daily;
    }

    public List<LabelCount> getTopUsers() {
        return topUsers;
    }

    public void setTopUsers(List<LabelCount> topUsers) {
        this.topUsers = topUsers;
    }

    public List<LabelCount> getFixModules() {
        return fixModules;
    }

    public void setFixModules(List<LabelCount> fixModules) {
        this.fixModules = fixModules;
    }
}
