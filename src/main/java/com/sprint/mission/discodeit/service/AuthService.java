package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.UserRoleUpdateRequest;
import java.util.UUID;

public interface AuthService {

    UserDto updateUserRole(UserRoleUpdateRequest userRoleUpdateRequest);

    void expireUpdatedUserSession(UUID userId);

    /* 로그인 기능은 filter 로 옮김 */
//    public UserDto login(LoginRequest loginRequest);
}
