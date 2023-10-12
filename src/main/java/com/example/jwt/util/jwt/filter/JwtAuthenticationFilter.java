package com.example.jwt.util.jwt.filter;

import com.example.jwt.util.jwt.service.CookiesService;
import com.example.jwt.util.jwt.service.JwtService;
import com.example.jwt.util.redis.service.RedisJwtBlacklistService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.example.jwt.util.AES.AESUtil.encrypt;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RedisJwtBlacklistService redisJwtBlacklistService;
    private final CookiesService cookiesService;

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);



    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/api/v1/auth/") || requestURI.startsWith("/ws")) {
            // /api/v1/auth/로 시작하는 요청에 대해서는 필터의 로직을 건너뛰고 다음 필터로 진행
            filterChain.doFilter(request, response);
            return;
        }

        

        String jwt = cookiesService.getAccessTokenFromCookies(request.getCookies());
        if(jwt == null){
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"invalid token\"}");
            return;
        }        

        final String userEmail;

        // redis로 블랙리스트 확인
        if(redisJwtBlacklistService.hasKeyBlackList(encrypt(jwt))) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Token has expired\"}");
            return;
        }

        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch(Exception error) {  // 어세스 토큰 만료 확인
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Token has expired\"}");
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        
        final String sessionTokenUserEmail;
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"invalid session token\"}");
            return;
        }
        final String sessionToken = authHeader.substring(7);

        try {
            sessionTokenUserEmail = jwtService.extractSessionTokenUsername(sessionToken);
            
        } catch(Exception error) {  // 어세스 토큰 만료 확인
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Session Token has expired\"}");
            return;
        }


            
        if(userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null && sessionTokenUserEmail != null && sessionTokenUserEmail.equals(userEmail)) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            if(jwtService.isTokenValid(jwt, userDetails) && jwtService.isSessionTokenValid(sessionToken, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        

        filterChain.doFilter(request, response);
    }
}
