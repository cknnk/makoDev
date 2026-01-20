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
    public void giveKudos(Long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow();

        if (task.getKudosCount() >= 2) {
            return;
        }

        // likes
        task.setKudosCount(task.getKudosCount() + 1);

        //cant kudos your own task (for the future)
        User author = task.getAssignee();

        // for now without id check (cuz 1 user)
        // if (currentUser.getId().equals(author.getId())) return;

        if (author != null) {
            author.setXp(author.getXp() + 5);

            //check for lvl up
            int newLevel = 1 + (author.getXp() / 100);
            if (newLevel > author.getLevel()) {
                author.setLevel(newLevel);
                //  maybe add message when user levels up?
            }

        }

        taskRepository.save(task);
        userRepository.save(author);
    }

    @Transactional
    public void completeTask(Long taskId, String username) {
        Task task = taskRepository.findById(taskId).orElseThrow();

        if ("DONE".equals(task.getStatus())) {
            return;
        }

        User currentUser = userRepository.findByUsername(username);

        task.setAssignee(currentUser);
        task.setStatus("DONE");

        int reward = task.getRewardXp();
        currentUser.setXp(currentUser.getXp() + reward);
        currentUser.setDailyXpEarned(currentUser.getDailyXpEarned() + reward);

        int newLevel = 1 + (currentUser.getXp() / 100); //maybe change xp needed from 100 to constantly raised (some formula?)
        if (newLevel > currentUser.getLevel()) {
            currentUser.setLevel(newLevel);
            //  maybe add message when user levels up?
        }

        userRepository.save(currentUser);
        taskRepository.save(task);
    }
}