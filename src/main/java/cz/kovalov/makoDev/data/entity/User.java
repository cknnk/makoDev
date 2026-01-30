package cz.kovalov.makoDev.data.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
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

    // gamef
    private int xp = 0;
    private int level = 1;
    private int coins = 0; //maybe for the shop

    // New: Anti-Burnout
    private int dailyXpEarned = 0;
    private int dailyKudosGiven = 0;
    private LocalDate lastActiveDate;

    @OneToMany(mappedBy = "assignee")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Task> tasks;

    @ManyToMany(mappedBy = "members")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Project> projects;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String email;

    @Column(length = 500)
    private String bio;

    @ManyToOne
    @JoinColumn(name = "current_project_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Project currentProject;

    public String getInitials() {
        if (firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()) {
            return (firstName.substring(0, 1) + lastName.substring(0, 1)).toUpperCase();
        }
        if (username != null && username.length() >= 2) {
            return username.substring(0, 2).toUpperCase();
        }
        return "GG";
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return username;
    }

    public void checkAndResetDailyStats() {
        LocalDate today = LocalDate.now();
        if (this.lastActiveDate == null || !this.lastActiveDate.equals(today)) {
            this.dailyXpEarned = 0;
            this.dailyKudosGiven = 0;
            this.lastActiveDate = today;
        }
    }
}