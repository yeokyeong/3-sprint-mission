package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.api.AuthApi;
import com.sprint.mission.discodeit.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthApi {

    private final AuthService authService;
    /* 로그인 기능은 filter 로 옮김 */
//    /* 유저 로그인 */
//    @PostMapping(path = "/login")
//    @Override
//    public ResponseEntity<UserDto> login(@RequestBody LoginRequest loginRequest) {
//        UserDto userDto = authService.login(loginRequest);
//        return ResponseEntity
//            .status(HttpStatus.OK)
//            .body(userDto);
//    }

    /*csrf 토큰 발급*/
    @GetMapping("/csrf-token")
    @Override
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
        log.debug("[CSRF 토큰 발급 요청]");
        log.debug("[CSRF 토큰 발급 완료]: - 헤더 이름: {}, - 토큰 값: {}", csrfToken.getHeaderName(),
            csrfToken.getToken());

        return ResponseEntity.noContent().build();
    }

}
