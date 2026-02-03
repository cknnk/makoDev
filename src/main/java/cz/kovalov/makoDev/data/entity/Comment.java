package cz.kovalov.makoDev.data.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Data
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String text;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User author;

    @ManyToOne
    @JoinColumn(name = "task_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Task task;

    public String getFormattedDate() {
        return createdAt.format(DateTimeFormatter.ofPattern("dd MMM HH:mm"));
    }
}