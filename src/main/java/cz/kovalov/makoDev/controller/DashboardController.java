package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.UserRepository;
import cz.kovalov.makoDev.service.ProjectService;
import cz.kovalov.makoDev.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

import java.util.List;

@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final TaskService taskService;
    private final ProjectService projectService;

    public DashboardController(UserRepository userRepository, TaskService taskService, ProjectService projectService) {
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.projectService = projectService;
    }

    @GetMapping("/")
    public String dashboard(Model model, Principal principal, HttpSession session) {
        String username = principal.getName();
        User currentUser = userRepository.findByUsername(username);

        if (currentUser == null) {
            session.invalidate();
            return "redirect:/login";
        }

        currentUser.checkAndResetDailyStats();
        userRepository.save(currentUser);

        model.addAttribute("user", currentUser);
        model.addAttribute("dailyXp", currentUser.getDailyXpEarned());
        model.addAttribute("maxDailyXp", 100);

        java.time.DayOfWeek day = java.time.LocalDate.now().getDayOfWeek();
        model.addAttribute("isWeekend", (day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY));

        Project activeProject = projectService.getActiveProject(currentUser, session);
        if (activeProject == null) {
            return "no-projects";
        }

        model.addAttribute("project", activeProject);
        model.addAttribute("teamMembers", activeProject.getMembers());

        boolean isOwner = activeProject.getOwner() != null && activeProject.getOwner().getId().equals(currentUser.getId());
        model.addAttribute("isProjectOwner", isOwner);

        model.addAttribute("todoTasks", taskService.getTodoTasks(activeProject));
        model.addAttribute("progressTasks", taskService.getProgressTasks(activeProject));
        model.addAttribute("reviewTasks", taskService.getReviewTasks(activeProject));
        model.addAttribute("doneTasks", taskService.getDoneTasks(activeProject));

        List<Task> allTasks = taskService.getAllProjectTasks(activeProject);
        int teamTotalXp = projectService.calculateProjectTotalXp(allTasks);

        model.addAttribute("projectLevel", projectService.calculateLevel(teamTotalXp));
        model.addAttribute("projectProgress", projectService.calculateProgress(teamTotalXp));

        return "index";
    }

    @GetMapping("/task/{id}/start")
    public String startTask(@PathVariable Long id, java.security.Principal principal) {
        taskService.startTask(id, principal.getName());
        return "redirect:/";
    }

    @GetMapping("/task/{id}/review")
    public String reviewTask(@PathVariable Long id, java.security.Principal principal) {
        taskService.sendToReview(id, principal.getName());
        return "redirect:/";
    }

    @GetMapping("/task/{id}/kudos")
    public String giveKudos(@PathVariable Long id, java.security.Principal principal) {
        taskService.giveKudos(id, principal.getName());
        return "redirect:/";
    }

    @GetMapping("/task/{id}/done")
    public String completeTask(@PathVariable Long id, java.security.Principal principal) {
        taskService.completeTask(id, principal.getName());
        return "redirect:/";
    }

    @PostMapping("/task/create")
    public String createTask(@RequestParam String title,
                             @RequestParam int reward,
                             @RequestParam String description,
                             @RequestParam String priority,
                             @RequestParam(required = false) String gitLink,
                             @RequestParam Long projectId,
                             java.security.Principal principal) {
        taskService.createTask(title, description, reward, priority, gitLink, projectId, principal.getName());
        return "redirect:/";
    }

    @PostMapping("/task/comment")
    public String addComment(@RequestParam Long taskId,
                             @RequestParam String text,
                             java.security.Principal principal) {
        taskService.addComment(taskId, text, principal.getName());
        return "redirect:/";
    }

    @PostMapping("/task/return")
    public String returnTask(@RequestParam Long taskId,
                             @RequestParam String reason,
                             java.security.Principal principal) {
        taskService.returnTask(taskId, reason, principal.getName());
        return "redirect:/";
    }

    @PostMapping("/task/delete")
    public String deleteTask(@RequestParam Long taskId, jakarta.servlet.http.HttpServletRequest request) {
        taskService.deleteTask(taskId);

        String referer = request.getHeader("Referer");
        if (referer == null || referer.isEmpty()) { return "redirect:/"; }
        return "redirect:" + referer;
    }
}