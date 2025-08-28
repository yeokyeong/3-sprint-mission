package com.sprint.mission.discodeit.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) {
        System.out.println("[JwtLogoutHandler] 로그아웃 처리 시작: 리프레시 쿠키 만료 시킴");

//        //1. access token 있으면 만료시킴
//        String authz = request.getHeader("Authorization");
//        if (authz != null && authz.startsWith("Bearer ")) {
//            String accessToken = authz.substring(7);
//            try {
//                String atJti = jwtTokenProvider.getTokenId(accessToken);
//                System.out.println("[JwtLogoutHandler] AT 즉시 폐기: jti=" + atJti);
//
//            } catch (Exception ignored) {
//            }
//        }
//        //2. refresh token 있으면 만료시킴
//        if (request.getCookies() != null) {
//            Optional<Cookie> rtCookie = Arrays.stream(request.getCookies())
//                .filter(c -> JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
//                .findFirst();
//            rtCookie.ifPresent(c -> {
//                try {
//                    String rtJti = jwtTokenProvider.getTokenId(c.getValue());
//                    System.out.println("[JwtLogoutHandler] RT 즉시 폐기: jti=" + rtJti);
//                } catch (Exception ignored) {
//                }
//            });
//        }

        System.out.println("[JwtLogoutHandler] 로그아웃 처리 완료: 만료 쿠키 전송");
        jwtTokenProvider.expireRefreshCookie(response);

    }
}
