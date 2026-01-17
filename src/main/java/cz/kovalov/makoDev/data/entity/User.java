package cz.kovalov.makoDev.data.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;

    private int xp = 0;
    private int level = 1;

    @OneToMany(mappedBy = "assignee")
    private List<Task> tasks;
}