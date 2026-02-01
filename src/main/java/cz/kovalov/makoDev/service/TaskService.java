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
    private static final int MAX_DAILY_KUDOS = 3;

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

    public void sendToReview(Long taskId, String username) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        User currentUser = userRepository.findByUsername(username);

        if (task.getAssignee() != null && !task.getAssignee().getId().equals(currentUser.getId())) {
            return;
        }

        //if bug
        if (task.getAssignee() == null) {
            task.setAssignee(currentUser);
        }

        task.setStatus("CODE_REVIEW");
        taskRepository.save(task);
    }

    @Transactional
    public void giveKudos(Long taskId, String username) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        User liker = userRepository.findByUsername(username);

        liker.checkAndResetDailyStats();

        User author = task.getAssignee();

        if (author != null && author.getId().equals(liker.getId())) { return; }
        if (liker.getDailyKudosGiven() >= MAX_DAILY_KUDOS) { return; }
        if (task.getKudoGivers().contains(liker)) { return; }

        task.getKudoGivers().add(liker);

        task.setKudosCount(task.getKudoGivers().size());

        liker.setDailyKudosGiven(liker.getDailyKudosGiven() + 1);
        userRepository.save(liker);

        if (author != null) {
            addXpWithBurnoutProtection(author, 5);
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
        java.time.DayOfWeek day = java.time.LocalDate.now().getDayOfWeek();
        if (day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY) {
            return;
        }

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