package com.example.herokuservice.dao;

import com.example.herokuservice.model.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author masoud
 */
@Repository
public interface ConfigDao extends JpaRepository<Config, UUID> {

}
