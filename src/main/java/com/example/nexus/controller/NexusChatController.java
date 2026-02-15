package com.example.nexus.controller;
import com.example.nexus.service.NexusChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NexusChatController {

    private final NexusChatService chatService;

    public NexusChatController(NexusChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String query) {
        return chatService.chat(query);
    }
}
