package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.TaskRepository;
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
import java.util.Comparator;

import java.util.List;

@Controller
public class DashboardController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final ProjectService projectService;

    public DashboardController(TaskRepository taskRepository, UserRepository userRepository, TaskService taskService, ProjectService projectService) {
        this.taskRepository = taskRepository;
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

        model.addAttribute("dailyXp", currentUser.getDailyXpEarned());
        model.addAttribute("maxDailyXp", 150);

        java.time.DayOfWeek day = java.time.LocalDate.now().getDayOfWeek();
        boolean isWeekend = (day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY);
        model.addAttribute("isWeekend", isWeekend);

        model.addAttribute("user", currentUser);

        Project activeProject = projectService.getActiveProject(currentUser, session);

        if (activeProject == null) {
            return "no-projects";
        }

        model.addAttribute("project", activeProject);
        model.addAttribute("teamMembers", activeProject.getMembers());

        boolean isOwner = activeProject.getOwner() != null && activeProject.getOwner().getId().equals(currentUser.getId());
        model.addAttribute("isProjectOwner", isOwner);

        List<Task> projectTasks = taskRepository.findByProject(activeProject);

        model.addAttribute("todoTasks", projectTasks.stream().filter(t -> "TODO".equals(t.getStatus())).toList());
        model.addAttribute("progressTasks", projectTasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).toList());
        model.addAttribute("reviewTasks", projectTasks.stream().filter(t -> "CODE_REVIEW".equals(t.getStatus())).toList());
        model.addAttribute("doneTasks", projectTasks.stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .sorted(Comparator.comparing(Task::getId).reversed())
                .toList());


        // gathering all users xp
        int teamTotalXp = projectService.calculateProjectTotalXp(projectTasks);

        //project level
        int projectLevel = 1 + (teamTotalXp / 1000);
        int projectProgress = teamTotalXp % 1000;

        model.addAttribute("projectLevel", projectLevel);
        model.addAttribute("projectProgress", projectProgress);

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

        String username = principal.getName();
        User currentUser = userRepository.findByUsername(username);

        Project project = currentUser.getProjects().stream()
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
        return "redirect:/";
    }

    @PostMapping("/task/comment")
    public String addComment(@RequestParam Long taskId,
                             @RequestParam String text,
                             java.security.Principal principal) {
        taskService.addComment(taskId, text, principal.getName());
        return "redirect:/";
    }
}