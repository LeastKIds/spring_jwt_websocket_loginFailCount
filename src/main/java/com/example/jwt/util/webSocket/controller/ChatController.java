package com.example.jwt.util.webSocket.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.example.jwt.util.webSocket.dto.response.ChatMessage;
import com.example.jwt.util.webSocket.service.ChatService;
import com.example.jwt.util.webSocket.type.i.WebSocketMessageInterface;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public WebSocketMessageInterface addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        return chatService.addUserService(chatMessage, headerAccessor);
    }
    
}
