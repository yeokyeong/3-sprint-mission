package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.DiscodeitUserDetails;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscodeitUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("[DiscodeitUserDetailsService] loadUserByUsername 실행");
        return userRepository.findByEmail(email)
            .map((user) -> new DiscodeitUserDetails(userMapper.toDto(user), user.getPassword()))
            .orElseThrow(
                () -> new UsernameNotFoundException("사용자 정보를 찾을수 없습니다. Email: " + email));
    }
}
