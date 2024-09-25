package com.big.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * TODO
 *
 * @author Yin
 * @date 2024-09-25 17:13
 */
@Entity
@Table(name = "t_demo_mojo")
public class DemoMojoModel {
    @Id
    @Column(unique = true)
    private String id;
}
