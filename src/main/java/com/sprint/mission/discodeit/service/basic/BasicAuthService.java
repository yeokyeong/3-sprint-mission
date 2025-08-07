package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.UserRoleUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.User.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BasicAuthService implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUserRole(UserRoleUpdateRequest userRoleUpdateRequest) {
        User user = userRepository.findById(userRoleUpdateRequest.userId()).orElseThrow(() ->
            new UserNotFoundException(userRoleUpdateRequest.userId().toString()));
        user.updateRole(userRoleUpdateRequest.newRole());

        return userMapper.toDto(user);
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
