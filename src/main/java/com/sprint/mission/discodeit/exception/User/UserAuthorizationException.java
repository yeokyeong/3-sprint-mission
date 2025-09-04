package com.sprint.mission.discodeit.exception.User;

import com.sprint.mission.discodeit.exception.ErrorCode;

public class UserAuthorizationException extends UserException {

    public UserAuthorizationException() {
        super(ErrorCode.UNAUTHORIZED_USER);
    }

    public UserAuthorizationException(String requestedUserName) {
        super(ErrorCode.UNAUTHORIZED_USER,
            String.format("요청 유저: Email=%s", requestedUserName));
    }
}
