package com.gt.user.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.gt.user.domain.entity.User;
import com.gt.user.domain.entity.UserRole;
import com.gt.user.domain.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;


    @Test
    public void save() {
        User user = new User("test@test.com", "test1234", "test", UserRole.ADMIN);
        User saveUser = userRepository.save(user);

        User findUser = userRepository.findById(saveUser.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        Assertions.assertThat(findUser.getEmail()).isEqualTo(user.getEmail());
    }
    
    @Test
    public void find(){

        User user = userRepository.findByEmail("test@test.com").get();
        Assertions.assertThat(user.getRole().getKey()).isEqualTo("ROLE_ADMIN");
    }
}
