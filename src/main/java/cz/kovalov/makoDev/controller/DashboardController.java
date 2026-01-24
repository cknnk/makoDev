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

        //1 attempt of getting project from session
        Long sessionProjectId = (Long) session.getAttribute("activeProjectId");
        if (sessionProjectId != null) {
            activeProject = currentUser.getProjects().stream()
                    .filter(p -> p.getId().equals(sessionProjectId))
                    .findFirst().orElse(null);
        }

        //2 attempt of getting project from database
        if (activeProject == null && currentUser.getCurrentProject() != null) {
            if (currentUser.getProjects().contains(currentUser.getCurrentProject())) {
                activeProject = currentUser.getCurrentProject();
                session.setAttribute("activeProjectId", activeProject.getId());
            }
        }

        // 3 attempt of getting project from any other available
        if (activeProject == null && !currentUser.getProjects().isEmpty()) {
            activeProject = currentUser.getProjects().get(0);

            session.setAttribute("activeProjectId", activeProject.getId());
            currentUser.setCurrentProject(activeProject);
            userRepository.save(currentUser);
        }

        //if no project then logout
        //fix it, redirect to no projects page maybe???
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