package com.zongkx.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

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
