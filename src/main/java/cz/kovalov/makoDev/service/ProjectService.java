package cz.kovalov.makoDev.service;

import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final UserRepository userRepository;

    private static final int REVIEW_REWARD = 15;

    public ProjectService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Project getActiveProject(User user, HttpSession session) {
        Project activeProject = null;

        // 1. session
        Long sessionProjectId = (Long) session.getAttribute("activeProjectId");
        if (sessionProjectId != null) {
            activeProject = user.getProjects().stream()
                    .filter(p -> p.getId().equals(sessionProjectId))
                    .findFirst().orElse(null);
        }

        // 2. database
        if (activeProject == null && user.getCurrentProject() != null) {
            if (user.getProjects().contains(user.getCurrentProject())) {
                activeProject = user.getCurrentProject();
                session.setAttribute("activeProjectId", activeProject.getId());
            }
        }

        // 3. any available one
        if (activeProject == null && !user.getProjects().isEmpty()) {
            activeProject = user.getProjects().get(0);

            session.setAttribute("activeProjectId", activeProject.getId());
            user.setCurrentProject(activeProject);
            userRepository.save(user);
        }

        // return null if project wasn't found
        return activeProject;
    }

    public int calculateProjectTotalXp(List<Task> projectTasks) {
        if (projectTasks == null || projectTasks.isEmpty()) {
            return 0;
        }

        // tasks
        int tasksXp = projectTasks.stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .mapToInt(Task::getRewardXp)
                .sum();

        int reviewXp = projectTasks.stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .filter(t -> t.getReviewer() != null)
                .mapToInt(t -> REVIEW_REWARD)
                .sum();

        // kudos
        int kudosXp = projectTasks.stream()
                .mapToInt(t -> t.getKudosCount() * 5)
                .sum();

        return tasksXp + reviewXp + kudosXp;
    }
}