package cz.kovalov.makoDev.service;

import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.TaskRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public void startTask(Long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus("IN_PROGRESS");
        taskRepository.save(task);
    }

    public void sendToReview(Long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus("CODE_REVIEW");
        taskRepository.save(task);
    }

    @Transactional
    public void completeTask(Long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow();

        if ("DONE".equals(task.getStatus())) {
            return;
        }

        task.setStatus("DONE");

        User user = task.getAssignee();
        int reward = task.getRewardXp();
        user.setXp(user.getXp() + reward);
        user.setDailyXpEarned(user.getDailyXpEarned() + reward);

        int newLevel = 1 + (user.getXp() / 100); //maybe change xp needed from 100 to constantly raised (some formula?)
        if (newLevel > user.getLevel()) {
            user.setLevel(newLevel);
            //  maybe add message when user levels up?
        }

        userRepository.save(user);
        taskRepository.save(task);
    }
}