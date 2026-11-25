package com.hospital.maintenance.service;

import com.hospital.maintenance.dto.AssistantChatRequest;
import com.hospital.maintenance.dto.AssistantChatResponse;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AssistantCommandService {

    private static final String UTF8 = "UTF-8";
    private static final Pattern DAMA_MONTH_YEAR = Pattern.compile("(?i)(generate|jana|download|export).*(report\\s+)?dama.*?(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\\s+(20\\d{2})");
    private static final Pattern DAMA_EXPLICIT_DATES = Pattern.compile("(?i)(generate|jana|download|export).*(report\\s+)?dama.*?(20\\d{2}-\\d{2}-\\d{2}).*?(20\\d{2}-\\d{2}-\\d{2})");

    public AssistantChatResponse tryHandle(AssistantChatRequest request) {
        if (request == null || request.getMessage() == null) {
            return null;
        }

        String message = request.getMessage().trim();
        AssistantChatResponse explicitDates = handleExplicitDates(message);
        if (explicitDates != null) {
            return explicitDates;
        }

        AssistantChatResponse monthYear = handleMonthYear(message);
        if (monthYear != null) {
            return monthYear;
        }

        return null;
    }

    private AssistantChatResponse handleExplicitDates(String message) {
        Matcher matcher = DAMA_EXPLICIT_DATES.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        String fromDate = matcher.group(3);
        String toDate = matcher.group(4);
        return buildDamaDownloadResponse(fromDate, toDate);
    }

    private AssistantChatResponse handleMonthYear(String message) {
        Matcher matcher = DAMA_MONTH_YEAR.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        String monthName = matcher.group(3);
        String year = matcher.group(4);
        String month = monthNumber(monthName);
        if (month == null) {
            return null;
        }

        String fromDate = year + "-" + month + "-01";
        String toDate = year + "-" + month + "-" + lastDayOfMonth(month, year);
        return buildDamaDownloadResponse(fromDate, toDate);
    }

    private AssistantChatResponse buildDamaDownloadResponse(String fromDate, String toDate) {
        AssistantChatResponse response = new AssistantChatResponse();
        response.setSuccess(true);
        response.setConfigured(true);
        response.setModel("local-command");
        response.setReply("Saya telah sediakan tindakan untuk `Data Diagnosis DAMA Bulanan` bagi tempoh `" + fromDate + "` hingga `" + toDate + "`. Klik butang di bawah untuk muat turun Excel.");
        response.setActionType("download_dama_report");
        response.setActionLabel("Download DAMA Excel");
        response.setDownloadUrl(buildDownloadUrl(fromDate, toDate));
        return response;
    }

    private String buildDownloadUrl(String fromDate, String toDate) {
        return "/api/reports/dama-diagnosis/export?fromDate="
                + encode(fromDate)
                + "&toDate="
                + encode(toDate);
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, UTF8);
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 encoding is not supported", ex);
        }
    }

    private String monthNumber(String monthName) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("january", "01");
        map.put("jan", "01");
        map.put("february", "02");
        map.put("feb", "02");
        map.put("march", "03");
        map.put("mar", "03");
        map.put("april", "04");
        map.put("apr", "04");
        map.put("may", "05");
        map.put("june", "06");
        map.put("jun", "06");
        map.put("july", "07");
        map.put("jul", "07");
        map.put("august", "08");
        map.put("aug", "08");
        map.put("september", "09");
        map.put("sep", "09");
        map.put("sept", "09");
        map.put("october", "10");
        map.put("oct", "10");
        map.put("november", "11");
        map.put("nov", "11");
        map.put("december", "12");
        map.put("dec", "12");
        return map.get(monthName.toLowerCase());
    }

    private String lastDayOfMonth(String month, String year) {
        if ("01".equals(month) || "03".equals(month) || "05".equals(month) || "07".equals(month)
                || "08".equals(month) || "10".equals(month) || "12".equals(month)) {
            return "31";
        }
        if ("04".equals(month) || "06".equals(month) || "09".equals(month) || "11".equals(month)) {
            return "30";
        }
        int numericYear = Integer.parseInt(year);
        boolean leap = (numericYear % 4 == 0 && numericYear % 100 != 0) || (numericYear % 400 == 0);
        return leap ? "29" : "28";
    }
}

