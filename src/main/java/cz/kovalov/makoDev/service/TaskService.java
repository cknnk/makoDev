package cz.kovalov.makoDev.service;

import cz.kovalov.makoDev.data.entity.Comment;
import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.TaskRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private static final int MAX_DAILY_XP = 100;
    private static final int REVIEW_REWARD = 15;
    private static final int MAX_DAILY_KUDOS = 3;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createTask(String title, String description, int reward, String priority, String gitLink, Long projectId, String username) {
        User currentUser = userRepository.findByUsername(username);

        cz.kovalov.makoDev.data.entity.Project project = currentUser.getProjects().stream()
                .filter(p -> p.getId().equals(projectId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setRewardXp(reward);
        task.setGitLink(gitLink);
        task.setStatus("TODO");
        task.setAssignee(currentUser);
        task.setProject(project);

        if ("HIGH".equals(priority) || "LOW".equals(priority)) {
            task.setPriority(priority);
        } else {
            task.setPriority("MEDIUM");
        }

        taskRepository.save(task);
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

        task.setStatus("DONE");
        task.setCompletedAt(java.time.LocalDateTime.now());
        task.setReviewer(reviewer);

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

    @Transactional
    public void addComment(Long taskId, String text, String username) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        User author = userRepository.findByUsername(username);

        Comment comment = new Comment();
        comment.setText(text);
        comment.setTask(task);
        comment.setAuthor(author);

        task.getComments().add(comment);
        taskRepository.save(task);
    }

    @Transactional
    public void returnTask(Long taskId, String reason, String reviewerUsername) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        User reviewer = userRepository.findByUsername(reviewerUsername);

        task.setStatus("CHANGES_REQUESTED");

        Comment comment = new Comment();
        comment.setText("🔴 REQUESTED CHANGES:\n" + reason);
        comment.setAuthor(reviewer);
        comment.setTask(task);

        task.getComments().add(comment);
        taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        taskRepository.deleteById(taskId);
    }

    public List<Task> getTodoTasks(Project project) {
        return taskRepository.findByProject(project).stream()
                .filter(t -> "TODO".equals(t.getStatus()))
                .sorted(Comparator.comparingInt(t -> {
                    String p = t.getPriority();
                    if ("HIGH".equals(p)) return 1;
                    if ("MEDIUM".equals(p)) return 2;
                    if ("LOW".equals(p)) return 3;
                    return 4;
                }))
                .toList();
    }

    public List<Task> getProgressTasks(Project project) {
        return taskRepository.findByProject(project).stream()
                .filter(t -> "IN_PROGRESS".equals(t.getStatus()) || "CHANGES_REQUESTED".equals(t.getStatus()))
                .toList();
    }

    public List<Task> getReviewTasks(Project project) {
        return taskRepository.findByProject(project).stream()
                .filter(t -> "CODE_REVIEW".equals(t.getStatus()))
                .toList();
    }

    public List<Task> getDoneTasks(Project project) {
        return taskRepository.findByProject(project).stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .sorted(Comparator.comparing(Task::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<Task> getAllProjectTasks(Project project) {
        return taskRepository.findByProject(project);
    }

    public int countDoneTasks(User user) {
        return taskRepository.countByAssigneeAndStatus(user, "DONE");
    }

    public int countTotalKudos(User user) {
        return taskRepository.getTotalKudosForUser(user);
    }

    public List<Task> getActivityLog(User user) {
        List<Task> completedTasks = taskRepository.findByAssigneeAndStatusOrderByIdDesc(user, "DONE");
        List<Task> reviewedTasks = taskRepository.findByReviewerAndStatusOrderByIdDesc(user, "DONE");

        List<Task> fullActivityLog = new java.util.ArrayList<>(completedTasks);
        fullActivityLog.addAll(reviewedTasks);

        fullActivityLog.sort(java.util.Comparator.comparing(Task::getCompletedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));

        return fullActivityLog;
    }
}