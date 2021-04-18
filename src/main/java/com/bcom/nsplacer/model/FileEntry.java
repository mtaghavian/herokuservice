package com.bcom.nsplacer.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@Table(name = "FileEntries")
public class FileEntry extends BaseModel {

    @Column(unique = true)
    private String name;

    @Column
    private Long length;

    @Column
    private UUID fileDataId;
}
