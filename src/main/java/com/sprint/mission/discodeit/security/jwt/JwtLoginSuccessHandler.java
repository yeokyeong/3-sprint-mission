package com.sprint.mission.discodeit.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.sprint.mission.discodeit.dto.data.DiscodeitUserDetails;
import com.sprint.mission.discodeit.dto.data.JwtDto;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.response.ErrorResponse;
import com.sprint.mission.discodeit.utils.SessionContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;
    private final SessionContext sessionContext;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException {
        System.out.println("[LoginSuccessHandler] 로그인 성공 처리 시작");
        if (authentication.getPrincipal() instanceof DiscodeitUserDetails discodeitUserDetails) {
            try {
                UserDto userDto = discodeitUserDetails.getUserDto();

                // JWT 토큰 발급
                String accessToken = jwtTokenProvider.generateAccessToken(discodeitUserDetails);
                String refreshToken = jwtTokenProvider.generateRefreshToken(discodeitUserDetails);

                // 응답 헤더 설정
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setStatus(HttpServletResponse.SC_OK);
                // refresh 토큰을 cookie로 설정
                jwtTokenProvider.addRefreshTokenAtCookie(response, refreshToken);

                //JSON 응답에 JwtDto값 추가
                String responseBody = objectMapper.writeValueAsString(
                    new JwtDto(userDto, accessToken));
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(responseBody);

                System.out.println(
                    "[LoginSuccessHandler] 로그인 성공 응답 완료(accessToken) :" + accessToken);

            } catch (JOSEException e) {
                log.error("Failed to generate JWT token for user: {}",
                    discodeitUserDetails.getUsername(), e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(Instant.now())
                    .message("Token generation failed")
                    .details(Map.of())
                    .build();
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            }

        } else {
            // 예상치 못한 principal인 경우
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .message("Authentication failed: Invalid user details")
                .details(Map.of())
                .build();
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            System.err.println(
                "[LoginSuccessHandler] 예상치 못한 Principal 타입: " + authentication.getPrincipal()
                    .getClass());

        }

    }
}
