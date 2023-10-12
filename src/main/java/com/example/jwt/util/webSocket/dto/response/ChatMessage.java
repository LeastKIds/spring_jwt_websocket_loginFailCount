package com.example.jwt.util.webSocket.dto.response;

import com.example.jwt.util.webSocket.type.e.MessageType;
import com.example.jwt.util.webSocket.type.i.WebSocketMessageInterface;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage implements WebSocketMessageInterface {
    private String content;
    private String sender;
    private MessageType type;
}
