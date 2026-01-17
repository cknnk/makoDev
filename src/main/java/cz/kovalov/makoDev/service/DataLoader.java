package cz.kovalov.makoDev.service;

import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.TaskRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public DataLoader(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User dev = new User();
            dev.setUsername("Maksym");
            dev.setPassword("secret"); // encoding - later
            dev.setXp(120);
            dev.setLevel(2);
            userRepository.save(dev);

            Task t1 = new Task();
            t1.setTitle("Fix Navigation Bug");
            t1.setStatus("TODO");
            t1.setRewardXp(50);
            t1.setAssignee(dev);
            taskRepository.save(t1);

            Task t2 = new Task();
            t2.setTitle("Write Documentation");
            t2.setStatus("IN_PROGRESS");
            t2.setRewardXp(20);
            t2.setAssignee(dev);
            taskRepository.save(t2);

            System.out.println("--- TEST DATA LOADED ---");
        }
    }
}