package com.hospital.maintenance.web;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.AssistantChatRequest;
import com.hospital.maintenance.dto.AssistantChatResponse;
import com.hospital.maintenance.service.AuthService;
import com.hospital.maintenance.service.OpenAiAssistantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final OpenAiAssistantService openAiAssistantService;

    public AssistantController(OpenAiAssistantService openAiAssistantService) {
        this.openAiAssistantService = openAiAssistantService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(@RequestBody AssistantChatRequest request,
                                                      HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(AuthService.SESSION_USER_KEY);
        if (user == null) {
            return new ResponseEntity<AssistantChatResponse>(HttpStatus.UNAUTHORIZED);
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<AssistantChatResponse>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(openAiAssistantService.chat(request, user));
    }
}
