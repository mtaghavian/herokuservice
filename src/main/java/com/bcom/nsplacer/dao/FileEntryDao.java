package com.bcom.nsplacer.dao;

import com.bcom.nsplacer.model.FileEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @author masoud
 */
@Repository
public interface FileEntryDao extends JpaRepository<FileEntry, UUID> {

    List<FileEntry> findByName(String name);
}
