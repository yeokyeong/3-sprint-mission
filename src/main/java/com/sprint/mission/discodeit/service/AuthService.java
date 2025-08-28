package com.sprint.mission.discodeit.service;

import com.nimbusds.jose.JOSEException;
import com.sprint.mission.discodeit.dto.data.JwtInformation;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.UserRoleUpdateRequest;

public interface AuthService {

    UserDto updateUserRole(UserRoleUpdateRequest userRoleUpdateRequest);


    JwtInformation refreshToken(String refreshToken) throws JOSEException;

    /* 로그인 기능은 filter 로 옮김 */
//    public UserDto login(LoginRequest loginRequest);
}
