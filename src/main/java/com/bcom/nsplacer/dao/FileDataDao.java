package com.bcom.nsplacer.dao;

import com.bcom.nsplacer.model.FileData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author masoud
 */
@Repository
public interface FileDataDao extends JpaRepository<FileData, UUID> {

}
