package com.bcom.nsplacer.heroku;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author masoud
 */
@Repository
public interface MessageDao extends JpaRepository<Message, UUID> {

}
