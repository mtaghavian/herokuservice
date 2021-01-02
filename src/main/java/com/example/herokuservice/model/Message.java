package com.example.herokuservice.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper=true)
@Table
public class Message extends BaseModel {

    @Column
    private String content;
}
