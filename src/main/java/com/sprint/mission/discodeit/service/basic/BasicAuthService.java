package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.DiscodeitUserDetails;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.UserRoleUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.User.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.AuthService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BasicAuthService implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SessionRegistry sessionRegistry;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUserRole(UserRoleUpdateRequest userRoleUpdateRequest) {
        User user = userRepository.findById(userRoleUpdateRequest.userId()).orElseThrow(() ->
            new UserNotFoundException(userRoleUpdateRequest.userId().toString()));
        user.updateRole(userRoleUpdateRequest.newRole());

        // role이 바뀐 유저는 세션 무효화
        this.expireUpdatedUserSession(userRoleUpdateRequest.userId());

        return userMapper.toDto(user);
    }

    @Override
    public void expireUpdatedUserSession(UUID userId) {
        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if (principal instanceof DiscodeitUserDetails userDetails) {
                if (userDetails.getUserDto().getId() == userId) {
                    List<SessionInformation> sessions = sessionRegistry.getAllSessions(userDetails,
                        false);
                    for (SessionInformation session : sessions) {
                        session.expireNow();

                    }
                }
            }
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
