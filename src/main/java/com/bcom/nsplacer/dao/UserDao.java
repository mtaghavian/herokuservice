package com.bcom.nsplacer.dao;

import com.bcom.nsplacer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserDao extends JpaRepository<User, UUID> {

    public User findByUsername(String username);
}
