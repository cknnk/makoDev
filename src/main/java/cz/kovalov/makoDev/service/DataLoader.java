/*
package cz.kovalov.makoDev.service;

import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.ProjectRepository;
import cz.kovalov.makoDev.data.repository.TaskRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public DataLoader(UserRepository userRepository, TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            // user
            User dev = new User();
            dev.setUsername("Maksym");
            dev.setPassword("{noop}secret"); // {noop} not encrypted yet
            dev.setXp(120);
            dev.setLevel(2);
            dev.setCoins(50);
            dev.setDailyXpEarned(0);
            dev.setLastActiveDate(LocalDate.now());
            userRepository.save(dev);

            // project
            Project project = new Project();
            project.setName("Diploma App");
            project.setDescription("Gamified Task Tracker system");
            project.setMembers(List.of(dev));
            projectRepository.save(project);

            // tasks
            createTask("Fix Login Bug", "TODO", 50, dev, project);
            createTask("Write Thesis Text", "IN_PROGRESS", 20, dev, project);
            createTask("Design Database", "DONE", 100, dev, project);
            createTask("Setup Spring Boot", "DONE", 30, dev, project);
            createTask("Review Code", "CODE_REVIEW", 40, dev, project);

            System.out.println("--- TEST DATA RELOADED WITH PROJECTS ---");
        }
    }

    private void createTask(String title, String status, int xp, User assignee, Project project) {
        Task t = new Task();
        t.setTitle(title);
        t.setStatus(status);
        t.setRewardXp(xp);
        t.setAssignee(assignee);
        t.setProject(project);
        t.setKudosCount(0);
        taskRepository.save(t);
    }
}*/
