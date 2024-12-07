package com.gt.user.domain.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import com.gt.user.domain.entity.User;


public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);
    
}
