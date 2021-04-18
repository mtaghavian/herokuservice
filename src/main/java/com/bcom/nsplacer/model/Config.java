package com.bcom.nsplacer.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import lombok.ToString;

@Entity
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper=true)
@Table
public class Config extends BaseModel {

    @Column
    private String name;

    @Column
    private String value;

}
