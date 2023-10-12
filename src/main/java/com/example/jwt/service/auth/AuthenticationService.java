package com.example.jwt.service.auth;

import static com.example.jwt.util.AES.AESUtil.decrypt;
import static com.example.jwt.util.AES.AESUtil.encrypt;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jwt.domain.auth.User;
import com.example.jwt.dto.request.auth.AuthenticationRequest;
import com.example.jwt.dto.request.auth.RegisterRequest;
import com.example.jwt.dto.response.auth.AuthenticationTokenResponse;
import com.example.jwt.dto.response.auth.LogoutResponse;
import com.example.jwt.error.ErrorResponse;
import com.example.jwt.repository.auth.UserRepository;
import com.example.jwt.service.token.TokenService;
import com.example.jwt.type.e.user.Role;
import com.example.jwt.type.i.auth.AuthenticateInterface;
import com.example.jwt.type.i.auth.LogoutInterface;
import com.example.jwt.type.i.auth.RefreshTokenInterface;
import com.example.jwt.type.i.auth.RegisterInterface;
import com.example.jwt.util.jwt.domain.RefreshToken;
import com.example.jwt.util.jwt.service.CookiesService;
import com.example.jwt.util.jwt.service.JwtService;
import com.example.jwt.util.jwt.type.RefreshTokenRepository;
import com.example.jwt.util.redis.service.RedisJwtBlacklistService;
import com.example.jwt.util.redis.service.RedisLoginFailCountService;
import com.example.jwt.validation.auth.AuthenticateValidation;
import com.example.jwt.validation.auth.RegisterValidation;
import com.example.jwt.validation.auth.TokenValidation;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository repository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;
    // private final RedisService redisService;
    private final RedisJwtBlacklistService redisJwtBlacklistService;
    private final RedisLoginFailCountService redisLoginFailCountService;

    private final UserRepository userRepository;

    private final TokenService tokenService;
    
    private final CookiesService cookiesService;

    // register
    @Transactional
    public ResponseEntity<RegisterInterface> register(RegisterRequest request, HttpServletResponse response) {

        // custom validation
        RegisterValidation registerValidation = new RegisterValidation();
        ErrorResponse errorResponse = registerValidation.registerValidation(request, userRepository);
        if(!errorResponse.getErrors().isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        
        // insert user information
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.User)
                .build();

        repository.save(user);

        // access token
        var jwtToken = jwtService.generateToken(user);

        // Refresh Token
        var refreshToken = jwtService.generateRefreshToken(user);

        // session token
        var sessionToken = jwtService.generateSessionToken(user);

        // return user information
        AuthenticationTokenResponse tokenResponse =
                AuthenticationTokenResponse.builder()
                        .firstname(user.getFirstname())
                        .lastname(user.getLastname())
                        .email(user.getEmail())
                        .sessionToken(sessionToken)
                        .role(user.getRole())
                        .build();


        response.addHeader("Set-Cookie", tokenService.getAccessTokenSetHeader(jwtToken));
        response.addHeader("Set-Cookie", tokenService.getRefreshTokenSetHeader(refreshToken));


        return ResponseEntity.ok(tokenResponse);
    }


    // login
    @Transactional
    public ResponseEntity<AuthenticateInterface> authenticate(AuthenticationRequest request, HttpServletResponse response) {
        AuthenticateValidation authenticateValidation = new AuthenticateValidation();

        ErrorResponse errorResponse = authenticateValidation.loginValidation(request);
        if(!errorResponse.getErrors().isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);


        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                ));
        } catch(BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authenticateValidation.authValidation(repository, redisLoginFailCountService, request.getEmail()));
        } catch(DisabledException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authenticateValidation.diableAuthError());
        } catch(LockedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authenticateValidation.authError());
        }


        User user;
        try {
                user = repository.findByEmail(request.getEmail())
                    .orElseThrow();
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(authenticateValidation.notFoundUserError());
        }
        
        var jwtToken = jwtService.generateToken(user);

        // Refresh Token
        var refreshToken = jwtService.generateRefreshToken(user);

        // session token
        var sessionToken = jwtService.generateSessionToken(user);


        response.addHeader("Set-Cookie", tokenService.getAccessTokenSetHeader(jwtToken));
        response.addHeader("Set-Cookie", tokenService.getRefreshTokenSetHeader(refreshToken));

        return ResponseEntity.ok(
                AuthenticationTokenResponse
                        .builder()
                        .firstname(user.getFirstname())
                        .lastname(user.getLastname())
                        .email(user.getEmail())
                        .sessionToken(sessionToken)
                        .role(user.getRole())
                        .build()
        );
    }

    // logout
    @Transactional
    public ResponseEntity<LogoutInterface> logout(HttpServletRequest request, HttpServletResponse response) throws RuntimeException{      
        TokenValidation tokenValidation = new TokenValidation();
        
        String accessJwt = cookiesService.getAccessTokenFromCookies(request.getCookies());
        String refreshJwt = cookiesService.getRefreshTokenFromCookies(request.getCookies());
        
        String userEmail;
        try {
            userEmail = jwtService.extractRefreshTokenUsername(refreshJwt);
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    tokenValidation.expiredToken());
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    tokenValidation.invalidToken());
        }

        if(!tokenValidation.findByToken(refreshJwt, refreshTokenRepository).getErrors().isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(tokenValidation.findByToken(refreshJwt, refreshTokenRepository));

        RefreshToken reToken = refreshTokenRepository.findByToken(encrypt(refreshJwt)).get();
        refreshTokenRepository.delete(reToken);

        Date expirationDate = jwtService.extractExpiration(accessJwt);
        Date currentDate = new Date();
        long differenceInMilliseconds = expirationDate.getTime() - currentDate.getTime();
        if (differenceInMilliseconds < 0) {
            // This means the token has already expired.
            differenceInMilliseconds = 0;
        }

        redisJwtBlacklistService.setBlackList(encrypt(accessJwt), userEmail, differenceInMilliseconds);

        response.addHeader("Set-Cookie", tokenService.deleteAccessTokenSetHeader(accessJwt));
        response.addHeader("Set-Cookie", tokenService.deleteRefreshTokenSetHeader(refreshJwt));

        return ResponseEntity.ok(LogoutResponse.builder().status(true).build());
    }

    // get access token
    @Transactional
    public ResponseEntity<RefreshTokenInterface> getAccessToken(HttpServletRequest request, HttpServletResponse response) {
        TokenValidation tokenValidation = new TokenValidation();

        String jwt = cookiesService.getRefreshTokenFromCookies(request.getCookies());
    
        String userEmail;
        try {
            userEmail = jwtService.extractRefreshTokenUsername(jwt);
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    tokenValidation.expiredToken());
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    tokenValidation.invalidToken());
        }

        var refresh = refreshTokenRepository.findByUserEmail(userEmail);
        if(refresh.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    tokenValidation.notFoundEmail());
        }

        if(jwt.equals(decrypt(refresh.get().getToken()))) {
            User user;

            try {
                 user = repository.findByEmail(userEmail)
                                    .orElseThrow();
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(tokenValidation.notEqulasToken());
            }

            
            var accessToken = jwtService.generateToken(user);
            var reGenerateRefreshToken = jwtService.generateRefreshToken(user);

            response.addHeader("Set-Cookie", tokenService.getAccessTokenSetHeader(accessToken));
            response.addHeader("Set-Cookie", tokenService.getRefreshTokenSetHeader(reGenerateRefreshToken));

            return ResponseEntity.ok(
                    AuthenticationTokenResponse
                            .builder()
                            .firstname(user.getFirstname())
                            .lastname(user.getLastname())
                            .email(user.getEmail())
                            .role(user.getRole())
                            .build()
            );
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    tokenValidation.notEqulasToken());
        }
    }

}
