package com.bcom.nsplacer.model;

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
@Table(name = "Messages")
public class Message extends BaseModel {

    @Column
    private String content;

    @Column
    private String type;

    @Column
    private String addr;

}
