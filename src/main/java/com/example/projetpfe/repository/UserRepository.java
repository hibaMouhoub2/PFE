package com.example.projetpfe.repository;

import com.example.projetpfe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    long countByRolesName(String roleName);
}
