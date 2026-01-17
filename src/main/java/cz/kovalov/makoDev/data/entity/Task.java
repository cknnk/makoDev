package cz.kovalov.makoDev.data.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String status; // "TODO", "DONE"

    private int rewardXp;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User assignee;
}