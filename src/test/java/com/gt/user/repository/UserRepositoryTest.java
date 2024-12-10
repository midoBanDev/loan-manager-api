package com.gt.user.repository;

import jakarta.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.gt.user.domain.entity.User;
import com.gt.user.domain.entity.UserRole;
import com.gt.user.domain.repository.UserRepository;

@SpringBootTest
@Transactional
public class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;

    @Test
    public void save() {
        User user = User.builder()
                .email("test@test.com")
                .name("test")
                .picture("test")
                .role(UserRole.ADMIN)
                .provider("test")
                .build();
        User saveUser = userRepository.save(user);

        User findUser = userRepository.findById(saveUser.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        Assertions.assertThat(findUser.getEmail()).isEqualTo(user.getEmail());
    }
    
    @Test
    public void find() {
        // 테스트 데이터 생성
        User user = User.builder()
                .email("test@test.com")
                .name("test")
                .picture("test")
                .role(UserRole.ADMIN)
                .provider("test")
                .build();
        userRepository.save(user);

        // 테스트 실행
        User foundUser = userRepository.findByEmail("test@test.com")
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 검증
        Assertions.assertThat(foundUser.getRole().getKey()).isEqualTo("ROLE_ADMIN");
    }
}
