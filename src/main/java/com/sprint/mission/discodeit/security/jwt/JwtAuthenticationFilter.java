package com.sprint.mission.discodeit.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.service.basic.DiscodeitUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

// 모든 HTTP 요청에서 Authentication 헤더 검사 유효한 jwt 토큰있으면 SecurityContext에서 인증정보로 설정
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final DiscodeitUserDetailsService discodeitUserDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getServletPath();             // getRequestURI 말고 이걸 권장
        return p.startsWith("/api/auth/refresh")         // ★ 리프레시 스킵
            || p.startsWith("/api/auth/login")
            || p.startsWith("/api/auth/csrf-token")
            || p.startsWith("/swagger-ui")
            || p.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        try {
            System.out.println("[JwtAuthenticationFilter] 요청 처리 시작: " + request.getMethod() + " "
                + request.getRequestURI());

            String token = resolveBearerToken(request);
            if (StringUtils.hasText(token)) {
                System.out.println("[JwtAuthenticationFilter] Bearer 토큰 추출 성공");

                // 토큰 유효성 검사
                if (jwtTokenProvider.validateAccessToken(token)) {
                    String username = jwtTokenProvider.getUsername(token);
                    UserDetails userDetails = discodeitUserDetailsService.loadUserByUsername(
                        username);

                    // 인증정보저장
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    System.out.println(
                        "[JwtAuthenticationFilter] SecurityContext 인증 설정 완료: username=" + username);

                } else {
                    // 토큰 유효성 검사 실패 시 처리(401)
                    System.out.println("[JwtAuthenticationFilter] 토큰 유효성 검사 실패");
                    send401Response(response, "토큰 유효성 검사 실패");
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("[JwtAuthenticationFilter] 예외 발생: " + e.getMessage());
            send401Response(response, "JWT 인증 실패");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorizationToken = request.getHeader("Authorization");
        System.out.println("authorizationToken : " + authorizationToken);
        if (StringUtils.hasText(authorizationToken) && authorizationToken.startsWith("Bearer ")) {
            System.out.println(
                "authorizationToken.substring(7) : " + authorizationToken.substring(7));
            return authorizationToken.substring(7);
        }
        return null;
    }

    private void send401Response(HttpServletResponse response, String message) throws IOException {
        // 응답 헤더 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // JSON 응답 전송
        String responseBody = objectMapper.createObjectNode()
            .put("success", false)
            .put("message", message)
            .toString();

        // 응답 바디 전송
        response.getWriter().write(responseBody);
    }
}
