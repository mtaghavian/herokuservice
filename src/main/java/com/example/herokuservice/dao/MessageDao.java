package com.example.herokuservice.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import com.example.herokuservice.model.Message;

/**
 * @author masoud
 */
@Repository
public interface MessageDao extends JpaRepository<Message, UUID> {

}
