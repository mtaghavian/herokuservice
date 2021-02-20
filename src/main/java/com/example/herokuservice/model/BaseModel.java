package com.example.herokuservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@MappedSuperclass
public class BaseModel {

    @Id
    @GeneratedValue
    @Column
    private UUID id;

    @Column
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private Date mdate;

    @PrePersist
    public void onPrePersist() {
        mdate = new Date();
    }

    @PreUpdate
    public void onPreUpdate() {
        mdate = new Date();
    }

}
