package com.sprint.mission.discodeit.dto.data;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor
public class DiscodeitUserDetails implements UserDetails {

    private final UserDto userDto;
    private final String password;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        System.out.println("[DiscodeitUserDetails] getAuthorities 실행");
        return List.of();
    }

    @Override
    public String getPassword() {
        System.out.println("[DiscodeitUserDetails] getPassword 실행");
        return password;
    }

    @Override
    public String getUsername() {
        System.out.println("[DiscodeitUserDetails] getUsername 실행");
        return userDto.getEmail();
    }


}
