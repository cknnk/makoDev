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
    private static final int REVIEW_REWARD = 15;

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

            updateLevel(author);
            userRepository.save(author);
        }

        taskRepository.save(task);
    }

    @Transactional
    public void completeTask(Long taskId, String reviewerUsername) {
        Task task = taskRepository.findById(taskId).orElseThrow();

        if ("DONE".equals(task.getStatus())) {
            return;
        }

        if (!"CODE_REVIEW".equals(task.getStatus())) {
            return;
        }

        User reviewer = userRepository.findByUsername(reviewerUsername);
        User author = task.getAssignee();

        // author can't approve his own task
        if (author != null && author.getId().equals(reviewer.getId())) {
            return;
        }

        // awarding the one who reviews
        addXpWithBurnoutProtection(reviewer, REVIEW_REWARD);

        if (author != null) {
            addXpWithBurnoutProtection(author, task.getRewardXp());
        }

        //maybe add a massage on lvl up?

        task.setStatus("DONE");
        taskRepository.save(task);
    }

    private void updateLevel(User user) {
        int newLevel = 1 + (user.getXp() / 100);
        if (newLevel > user.getLevel()) {
            user.setLevel(newLevel);
        }
    }

    private void addXpWithBurnoutProtection(User user, int amount) {
        user.checkAndResetDailyStats();

        int currentDaily = user.getDailyXpEarned();
        int xpToAdd;

        if (currentDaily >= MAX_DAILY_XP) {
            xpToAdd = 0;
        } else if (currentDaily + amount > MAX_DAILY_XP) {
            xpToAdd = MAX_DAILY_XP - currentDaily;
        } else {
            xpToAdd = amount;
        }

        if (xpToAdd > 0) {
            user.setXp(user.getXp() + xpToAdd);
            user.setDailyXpEarned(user.getDailyXpEarned() + xpToAdd);
            updateLevel(user);
            userRepository.save(user);
        }
    }
}