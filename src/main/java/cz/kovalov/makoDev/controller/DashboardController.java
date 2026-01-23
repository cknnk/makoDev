package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.TaskRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
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

    public DashboardController(TaskRepository taskRepository, UserRepository userRepository, TaskService taskService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskService = taskService;
    }

    @GetMapping("/")
    public String dashboard(Model model, Principal principal, HttpSession session) {
        String username = principal.getName();
        User currentUser = userRepository.findByUsername(username);

        if (currentUser == null) {
            session.invalidate();
            return "redirect:/login";
        }


        model.addAttribute("user", currentUser);

        Project activeProject = null;

        Long activeProjectId = (Long) session.getAttribute("activeProjectId");

        if (activeProjectId != null) {
            activeProject = currentUser.getProjects().stream()
                    .filter(p -> p.getId().equals(activeProjectId))
                    .findFirst()
                    .orElse(null);
        }

        if (activeProject == null && !currentUser.getProjects().isEmpty()) {
            activeProject = currentUser.getProjects().get(0);
            session.setAttribute("activeProjectId", activeProject.getId());
        }

        //fix it, redirect to no projects page
        if (activeProject == null) {
            return "redirect:/logout";
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

        return "index";
    }

    @GetMapping("/task/{id}/start")
    public String startTask(@PathVariable Long id, java.security.Principal principal) {
        taskService.startTask(id, principal.getName());
        return "redirect:/";
    }

    @GetMapping("/task/{id}/review")
    public String reviewTask(@PathVariable Long id) {
        taskService.sendToReview(id);
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
        task.setStatus("TODO");
        task.setAssignee(currentUser);
        task.setProject(project);

        taskRepository.save(task);
        return "redirect:/";
    }
}