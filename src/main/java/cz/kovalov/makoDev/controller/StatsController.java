package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.service.ProjectService;
import cz.kovalov.makoDev.service.TaskService;
import cz.kovalov.makoDev.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
public class StatsController {

    private final UserService userService;
    private final ProjectService projectService;
    private final TaskService taskService;

    public StatsController(UserService userService, ProjectService projectService, TaskService taskService) {
        this.userService = userService;
        this.projectService = projectService;
        this.taskService = taskService;
    }

    @GetMapping("/stats")
    public String teamStats(Model model, Principal principal, HttpSession session) {
        User currentUser = userService.getDashboardUser(principal.getName());

        Project activeProject = projectService.getActiveProject(currentUser, session);
        if (activeProject == null) return "redirect:/";

        List<Task> allTasks = taskService.getAllProjectTasks(activeProject);

        int teamTotalXp = projectService.calculateProjectTotalXp(allTasks);

        model.addAttribute("teamTotalXp", teamTotalXp);
        model.addAttribute("projectLevel", projectService.calculateLevel(teamTotalXp));
        model.addAttribute("progressToNextLevel", projectService.calculateProgress(teamTotalXp));

        int myTotalContribution = projectService.calculateUserContributionXp(allTasks, currentUser);
        long myTasksCount = taskService.countUserDoneTasks(allTasks, currentUser);

        double myContributionPercent = (teamTotalXp == 0) ? 0 : ((double) myTotalContribution / teamTotalXp) * 100;

        List<Task> recentlyDone = taskService.getRecentProjectActivity(activeProject);

        model.addAttribute("user", currentUser);
        model.addAttribute("project", activeProject);
        model.addAttribute("myContribution", (int) myContributionPercent);
        model.addAttribute("myTasksCount", myTasksCount);
        model.addAttribute("recentActivity", recentlyDone);

        return "stats";
    }
}