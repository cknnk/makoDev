package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.TaskRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
import cz.kovalov.makoDev.service.ProjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;

@Controller
public class StatsController {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ProjectService projectService;

    public StatsController(UserRepository userRepository, TaskRepository taskRepository, ProjectService projectService) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.projectService = projectService;
    }

    @GetMapping("/stats")
    public String teamStats(Model model, Principal principal, HttpSession session) {
        User currentUser = userRepository.findByUsername(principal.getName());

        Project activeProject = projectService.getActiveProject(currentUser, session);

        if (activeProject == null) return "redirect:/";

        List<Task> allTasks = taskRepository.findByProject(activeProject);

        // total xp
        int teamTotalXp = projectService.calculateProjectTotalXp(allTasks);

        int projectLevel = 1 + (teamTotalXp / 1000);
        int progressToNextLevel = teamTotalXp % 1000;

        int myTaskRewardXp = allTasks.stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(currentUser.getId()))
                .mapToInt(Task::getRewardXp)
                .sum();

        int myKudosXp = allTasks.stream()
                .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(currentUser.getId()))
                .mapToInt(t -> t.getKudosCount() * 5)
                .sum();

        int myReviewXp = allTasks.stream()
                .filter(t -> "DONE".equals(t.getStatus())).anyMatch(t -> t.getReviewer() != null && t.getReviewer().getId().equals(currentUser.getId())) ?
                (int) allTasks.stream()
                        .filter(t -> "DONE".equals(t.getStatus()))
                        .filter(t -> t.getReviewer() != null && t.getReviewer().getId().equals(currentUser.getId()))
                        .count() * 15
                : 0;

        int myTotalContribution = myTaskRewardXp + myKudosXp + myReviewXp;

        double myContributionPercent = (teamTotalXp == 0) ? 0 : ((double) myTotalContribution / teamTotalXp) * 100;

        // my tasks count
        long myTasksCount = allTasks.stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(currentUser.getId()))
                .count();

        // last finished tasks (activity log)
        List<Task> recentlyDone = allTasks.stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .sorted(Comparator.comparing(Task::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();

        model.addAttribute("user", currentUser);
        model.addAttribute("project", activeProject);

        model.addAttribute("teamTotalXp", teamTotalXp);
        model.addAttribute("projectLevel", projectLevel);
        model.addAttribute("progressToNextLevel", progressToNextLevel);

        model.addAttribute("myContribution", (int) myContributionPercent);
        model.addAttribute("myTasksCount", myTasksCount);

        model.addAttribute("recentActivity", recentlyDone);

        return "stats";
    }
}