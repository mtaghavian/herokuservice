package com.bcom.nsplacer.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@Table(name = "FileDatas")
public class FileData extends BaseModel {

    public static final int MAX_SIZE = 20000000;

    @Column
    private UUID next;

    @Lob
    private byte data[];

    public FileData(UUID id) {
        super();
        setId(id);
    }
}
