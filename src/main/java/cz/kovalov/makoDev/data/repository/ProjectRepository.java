package cz.kovalov.makoDev.data.repository;

import cz.kovalov.makoDev.data.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}