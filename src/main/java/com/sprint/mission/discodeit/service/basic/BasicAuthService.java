package com.sprint.mission.discodeit.service.basic;

import com.nimbusds.jose.JOSEException;
import com.sprint.mission.discodeit.dto.data.DiscodeitUserDetails;
import com.sprint.mission.discodeit.dto.data.JwtInformation;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.UserRoleUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.Common.InternalServerException;
import com.sprint.mission.discodeit.exception.Common.SecurityException;
import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.exception.User.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.security.jwt.JwtTokenProvider;
import com.sprint.mission.discodeit.service.AuthService;
import com.sprint.mission.discodeit.utils.SessionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BasicAuthService implements AuthService {

    private final UserRepository userRepository;
    private final DiscodeitUserDetailsService discodeitUserDetailsService;
    private final UserMapper userMapper;
    private final SessionContext sessionContext;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUserRole(UserRoleUpdateRequest userRoleUpdateRequest) {
        User user = userRepository.findById(userRoleUpdateRequest.userId()).orElseThrow(() ->
            new UserNotFoundException(userRoleUpdateRequest.userId().toString()));
        user.updateRole(userRoleUpdateRequest.newRole());

        // role이 바뀐 유저는 세션 무효화
        sessionContext.expireUpdatedUserSession(userRoleUpdateRequest.userId());

        // status 정보
        boolean isOnline = sessionContext.getStatusFromSession(user.getId());
        return userMapper.toDto(user, isOnline);
    }

    @Override
    public JwtInformation refreshToken(String refreshToken) {
        System.out.println("refreshToken 검증 시작" + refreshToken);
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new SecurityException(ErrorCode.INVALID_TOKEN.getDefaultMessage());
        }

        String username = jwtTokenProvider.getUsername(refreshToken);
        UserDetails userDetails = discodeitUserDetailsService.loadUserByUsername(username);

        if (!(userDetails instanceof DiscodeitUserDetails discodeitUserDetails)) {
            throw new SecurityException(ErrorCode.INVALID_USER_DETAILS.getDefaultMessage());
        }

        try {
            String newAccessToken = jwtTokenProvider.generateAccessToken(discodeitUserDetails);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(discodeitUserDetails);
            System.out.println(
                "refreshToken, newAccessToken 재발급 완료 " + "\n AT:" + newAccessToken + "\n RT:"
                    + newRefreshToken);

            return new JwtInformation(
                discodeitUserDetails.getUserDto(),
                newAccessToken,
                newRefreshToken
            );
        } catch (JOSEException e) {
            throw new InternalServerException();
        }
    }

    /* 로그인 기능은 filter 로 옮김 */
//    @Transactional
//    @Override
//    public UserDto login(LoginRequest loginRequest) {
//        User user = this.userRepository.findByUsername(loginRequest.username()).orElseThrow(
//            () -> new UserNotFoundException(loginRequest.username()));
//
//        if (!user.getPassword().equals(loginRequest.password())) {
//            throw new InvalidCredentialsException(loginRequest.username(), loginRequest.password());
//        }
//
//        return userMapper.toDto(user);
//    }

}
