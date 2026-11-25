package com.hospital.maintenance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.AssistantChatMessage;
import com.hospital.maintenance.dto.AssistantChatRequest;
import com.hospital.maintenance.dto.AssistantChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAssistantService.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final LocalAssistantFallbackService localAssistantFallbackService;
    private final AssistantCommandService assistantCommandService;
    private final String openAiApiKey;
    private final String openAiModel;
    private final String openAiResponsesUrl;

    public OpenAiAssistantService(ObjectMapper objectMapper,
                                  LocalAssistantFallbackService localAssistantFallbackService,
                                  AssistantCommandService assistantCommandService,
                                  @Value("${OPENAI_API_KEY:}") String openAiApiKey,
                                  @Value("${OPENAI_MODEL:gpt-4.1-mini}") String openAiModel,
                                  @Value("${OPENAI_RESPONSES_URL:https://api.openai.com/v1/responses}") String openAiResponsesUrl) {
        this.objectMapper = objectMapper;
        this.localAssistantFallbackService = localAssistantFallbackService;
        this.assistantCommandService = assistantCommandService;
        this.openAiApiKey = openAiApiKey;
        this.openAiModel = openAiModel;
        this.openAiResponsesUrl = openAiResponsesUrl;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000);
        requestFactory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public AssistantChatResponse chat(AssistantChatRequest request, SystemUser user) {
        AssistantChatResponse response = new AssistantChatResponse();
        response.setModel(openAiModel);

        if (request == null || isBlank(request.getMessage())) {
            response.setSuccess(false);
            response.setConfigured(isConfigured());
            response.setReply("Sila masukkan soalan terlebih dahulu.");
            return response;
        }

        AssistantChatResponse commandResponse = assistantCommandService.tryHandle(request);
        if (commandResponse != null) {
            return commandResponse;
        }

        if (!isConfigured()) {
            response.setSuccess(true);
            response.setConfigured(false);
            response.setReply(localAssistantFallbackService.reply(request.getMessage(), user,
                    "OpenAI API belum dikonfigurasi. Saya jawab menggunakan fallback knowledge tempatan."));
            return response;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openAiApiKey);

            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("model", openAiModel);
            payload.put("store", Boolean.FALSE);
            payload.put("instructions", buildInstruction(user));
            payload.put("input", buildConversationPrompt(request));
            payload.put("text", textFormat());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(payload, headers);
            ResponseEntity<String> apiResponse = restTemplate.exchange(openAiResponsesUrl, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(apiResponse.getBody());

            response.setSuccess(true);
            response.setConfigured(true);
            response.setReply(extractReply(root));
            return response;
        } catch (HttpStatusCodeException ex) {
            log.warn("OpenAI API returned status {} with body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            response.setSuccess(true);
            response.setConfigured(true);
            if (ex.getStatusCode().value() == 429) {
                response.setReply(localAssistantFallbackService.reply(request.getMessage(), user,
                        "OpenAI quota/billing/rate limit sedang menyekat permintaan. Saya jawab menggunakan fallback knowledge tempatan."));
            } else {
                response.setReply(buildHttpErrorMessage(ex));
            }
            return response;
        } catch (ResourceAccessException ex) {
            log.warn("OpenAI API connection failed", ex);
            response.setSuccess(true);
            response.setConfigured(true);
            response.setReply(localAssistantFallbackService.reply(request.getMessage(), user,
                    "OpenAI API tidak dapat dicapai sekarang. Saya jawab menggunakan fallback knowledge tempatan."));
            return response;
        } catch (Exception ex) {
            log.error("Unexpected AI Assistant error", ex);
            response.setSuccess(true);
            response.setConfigured(true);
            response.setReply(localAssistantFallbackService.reply(request.getMessage(), user,
                    "OpenAI API menghadapi masalah sementara. Saya jawab menggunakan fallback knowledge tempatan."));
            return response;
        }
    }

    private Map<String, Object> textFormat() {
        Map<String, Object> format = new LinkedHashMap<String, Object>();
        format.put("type", "text");

        Map<String, Object> text = new LinkedHashMap<String, Object>();
        text.put("format", format);
        return text;
    }

    private String buildConversationPrompt(AssistantChatRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Perbualan setakat ini:\n");

        List<AssistantChatMessage> history = request.getHistory();
        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                AssistantChatMessage item = history.get(i);
                if (item == null || isBlank(item.getContent())) {
                    continue;
                }
                String speaker = "assistant".equalsIgnoreCase(item.getRole()) ? "AI" : "User";
                prompt.append(speaker).append(": ").append(item.getContent()).append("\n");
            }
        }

        prompt.append("User: ").append(request.getMessage()).append("\n");
        prompt.append("Sila jawab soalan user berdasarkan konteks sistem HSI Utilities.");
        return prompt.toString();
    }

    private String buildInstruction(SystemUser user) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Anda ialah AI Assistant untuk sistem HSI Utilities. ");
        prompt.append("Jawab dalam Bahasa Melayu yang ringkas, jelas, dan mesra kecuali user minta bahasa lain. ");
        prompt.append("Anda hanya membantu secara read-only dan tidak boleh mendakwa telah mengubah database, report, atau konfigurasi. ");
        prompt.append("Fokus bantuan pada penggunaan sistem, modul report, deploy checklist, dan troubleshooting asas. ");
        prompt.append("Modul semasa yang diketahui: Dashboard, HSI Reports, Audit Log, PD103, dan Data Diagnosis DAMA Bulanan. ");
        prompt.append("Report lain masih belum siap sepenuhnya kecuali jika dinyatakan. ");
        prompt.append("Jika user meminta generate report sebenar atau query database, terangkan langkah yang perlu dibuat dalam sistem dan jangan mendakwa anda telah menjalankannya, kecuali sistem memang memulangkan tindakan download yang sah. ");
        if (user != null && !isBlank(user.getFullName())) {
            prompt.append("User semasa ialah ").append(user.getFullName()).append(".");
        }
        return prompt.toString();
    }

    private String extractReply(JsonNode root) {
        if (root == null) {
            return "Tiada jawapan diterima daripada model.";
        }

        JsonNode outputText = root.get("output_text");
        if (outputText != null && !outputText.isNull() && !isBlank(outputText.asText())) {
            return outputText.asText();
        }

        JsonNode output = root.get("output");
        if (output != null && output.isArray()) {
            for (int i = 0; i < output.size(); i++) {
                JsonNode item = output.get(i);
                JsonNode content = item.get("content");
                if (content != null && content.isArray()) {
                    for (int j = 0; j < content.size(); j++) {
                        JsonNode part = content.get(j);
                        if (part != null && "output_text".equals(part.path("type").asText()) && !isBlank(part.path("text").asText())) {
                            return part.path("text").asText();
                        }
                    }
                }
            }
        }

        return "Tiada jawapan teks diterima daripada model.";
    }

    private String buildHttpErrorMessage(HttpStatusCodeException ex) {
        int status = ex.getStatusCode().value();
        if (status == 401) {
            return "OpenAI API key tidak sah atau tidak dibenarkan. Sila semak `OPENAI_API_KEY`.";
        }
        if (status == 429) {
            return "Permintaan ke OpenAI disekat kerana quota/billing/rate limit. Sila semak akaun API anda.";
        }
        if (status >= 500) {
            return "OpenAI API sedang menghadapi masalah sementara. Cuba lagi sebentar.";
        }
        return "OpenAI API memulangkan ralat HTTP " + status + ". Sila semak model, API key, atau payload request.";
    }

    private boolean isConfigured() {
        return !isBlank(openAiApiKey);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
