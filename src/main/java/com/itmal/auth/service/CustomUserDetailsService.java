package com.itmal.auth.service;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.auth.domain.User;
import com.itmal.auth.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 이메일입니다: " + email));

        return new CustomUserDetails(user);
    }
}
