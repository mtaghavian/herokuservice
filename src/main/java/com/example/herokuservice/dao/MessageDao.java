package com.example.herokuservice.dao;

import com.example.herokuservice.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageDao extends JpaRepository<Message, UUID> {

}
