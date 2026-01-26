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

    private static final int MAX_DAILY_XP = 150;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void startTask(Long taskId, String username) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        User currentUser = userRepository.findByUsername(username);

        task.setAssignee(currentUser);

        task.setStatus("IN_PROGRESS");

        taskRepository.save(task);
    }

    public void sendToReview(Long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus("CODE_REVIEW");
        taskRepository.save(task);
    }

    @Transactional
    public void giveKudos(Long taskId, String username) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        User liker = userRepository.findByUsername(username);
        User author = task.getAssignee();

        if (author != null && author.getId().equals(liker.getId())) { return; }
        if (task.getKudosCount() >= 1) { return; }

        task.setKudosCount(task.getKudosCount() + 1);

        if (author != null) {
            author.setXp(author.getXp() + 5);

            int newLevel = 1 + (author.getXp() / 100);
            if (newLevel > author.getLevel()) {
                author.setLevel(newLevel);
            }
            userRepository.save(author);
        }

        taskRepository.save(task);
    }

    @Transactional
    public void completeTask(Long taskId, String username) {
        Task task = taskRepository.findById(taskId).orElseThrow();

        if ("DONE".equals(task.getStatus())) {
            return;
        }

        User currentUser = userRepository.findByUsername(username);

        currentUser.checkAndResetDailyStats();

        task.setAssignee(currentUser);
        task.setStatus("DONE");

        // anti-burnout logic
        int reward = task.getRewardXp();
        int currentDaily = currentUser.getDailyXpEarned();
        int xpToAdd;

        if (currentDaily >= MAX_DAILY_XP) {
            xpToAdd = 0;
        } else if (currentDaily + reward > MAX_DAILY_XP) {
            xpToAdd = MAX_DAILY_XP - currentDaily;
        } else {
            xpToAdd = reward;
        }

        if (xpToAdd > 0) {
            currentUser.setXp(currentUser.getXp() + xpToAdd);
            currentUser.setDailyXpEarned(currentUser.getDailyXpEarned() + xpToAdd);

            int newLevel = 1 + (currentUser.getXp() / 100);
            if (newLevel > currentUser.getLevel()) { //maybe change xp needed from 100 to constantly raised (some formula?)
                currentUser.setLevel(newLevel);
                //  maybe add message when user levels up?
            }
        }

        userRepository.save(currentUser);
        taskRepository.save(task);
    }
}