package com.gt.user.application.service;

import com.gt.user.domain.entity.User;
import com.gt.user.domain.entity.UserRole;
import com.gt.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User findOrCreateGoogleUser(String email, String name, String pictureUrl) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    user.updateProfile(name, pictureUrl);
                    return user;
                })
                .orElseGet(() -> createGoogleUser(email, name, pictureUrl));
    }

    private User createGoogleUser(String email, String name, String pictureUrl) {
        User user = User.builder()
                .email(email)
                .name(name)
                .picture(pictureUrl)
                .role(UserRole.USER)
                .provider("google")
                .build();
        return userRepository.save(user);
    }
} 