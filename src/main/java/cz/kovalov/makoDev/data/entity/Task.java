package cz.kovalov.makoDev.data.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    // "TODO", "IN_PROGRESS", "CODE_REVIEW", "DONE"
    private String status;

    private int rewardXp;

    private int kudosCount = 0;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User assignee;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
}