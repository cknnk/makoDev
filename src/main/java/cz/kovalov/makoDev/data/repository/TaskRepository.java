package cz.kovalov.makoDev.data.repository;

import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    int countByAssigneeAndStatus(User assignee, String status);

    @Query("SELECT COALESCE(SUM(t.kudosCount), 0) FROM Task t WHERE t.assignee = :user")
    Integer getTotalKudosForUser(@Param("user") User user);

    List<Task> findByProject(Project project);

    List<Task> findByAssigneeAndStatusOrderByIdDesc(User assignee, String status);

    List<Task> findByReviewerAndStatusOrderByIdDesc(User reviewer, String status);
}