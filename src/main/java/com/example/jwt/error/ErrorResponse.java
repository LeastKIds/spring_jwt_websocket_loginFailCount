package com.example.jwt.error;

import java.util.Map;

import com.example.jwt.type.i.auth.AuthenticateInterface;
import com.example.jwt.type.i.auth.LogoutInterface;
import com.example.jwt.type.i.auth.RefreshTokenInterface;
import com.example.jwt.type.i.auth.RegisterInterface;
import com.example.jwt.util.webSocket.type.i.WebSocketMessageInterface;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse
        implements RefreshTokenInterface,
        LogoutInterface,
        RegisterInterface,
        AuthenticateInterface, 
        WebSocketMessageInterface {
    private Map<String, String> errors;
}
