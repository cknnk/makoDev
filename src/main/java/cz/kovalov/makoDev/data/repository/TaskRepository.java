package cz.kovalov.makoDev.data.repository;

import cz.kovalov.makoDev.data.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

//@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(String status);
}