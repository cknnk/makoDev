package cz.kovalov.makoDev.data.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)
    private String description;

    // "TODO", "IN_PROGRESS", "CODE_REVIEW", "DONE"
    private String status;

    private int rewardXp;

    private int kudosCount = 0;

    private String gitLink;
    private LocalDateTime completedAt;

    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH

    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User assignee;

    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User reviewer;

    @ManyToOne
    @JoinColumn(name = "project_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Project project;

    //set - to avoid duplicates
    @ManyToMany
    @JoinTable(
            name = "task_kudos",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<User> kudoGivers = new HashSet<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<Comment> comments = new java.util.ArrayList<>();
}