
package com.example.herokuservice.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import com.example.herokuservice.model.Config;

/**
 * @author masoud
 */
@Repository
public interface ConfigDao extends JpaRepository<Config, UUID> {

}
