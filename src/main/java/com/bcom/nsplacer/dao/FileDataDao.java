package com.bcom.nsplacer.dao;

import com.bcom.nsplacer.model.FileData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * @author masoud
 */
@Repository
public interface FileDataDao extends JpaRepository<FileData, UUID> {

    @Query(value = "select data.\"id\", data.\"next\" from \"file_datas\" data where data.\"id\" = :recordId", nativeQuery = true)
    String fetchIdAndNext(@Param("recordId") UUID recordId);

    @Transactional
    @Modifying
    @Query(value = "delete from \"file_datas\" data where data.\"id\" = :recordId", nativeQuery = true)
    void deleteWithoutLoading(@Param("recordId") UUID recordId);
}
