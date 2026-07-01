package com.itmal.auth.repository;

import com.itmal.auth.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {
    void insert(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(@Param("userId") Long userId);

    Optional<User> findByProviderAndProviderId(@Param("provider") String provider, @Param("providerId") String providerId);

    void updateProvider(@Param("userId") Long userId, @Param("provider") String provider, @Param("providerId") String providerId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    void updateProfile(@Param("userId") Long userId, @Param("nickname") String nickname, @Param("nativeLanguage") String nativeLanguage);

    void updatePassword(@Param("userId") Long userId, @Param("password") String password);

    void softDelete(@Param("userId") Long userId);

    void updateRole(@Param("userId") Long userId, @Param("role") String role);
}
