package com.zongkx.jpa.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_user")
@Data
public class User {
    @Id
    private Long id;
    @Column
    private String name;
    @Column
    private String type;
    @Column
    private LocalDateTime start;
}
