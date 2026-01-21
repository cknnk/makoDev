package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.TaskRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
import cz.kovalov.makoDev.service.TaskService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String dashboard(Model model, java.security.Principal principal) {
        String username = principal.getName();
        User currentUser = userRepository.findByUsername(username);
        model.addAttribute("user", currentUser);

        //for now grab first project (change in future)
        if (currentUser.getProjects().isEmpty()) {
            //just in case
            return "redirect:/logout";
        }
        Project currentProject = currentUser.getProjects().get(0);

        model.addAttribute("project", currentProject);
        model.addAttribute("teamMembers", currentProject.getMembers());

        List<Task> allTasks = taskRepository.findAll();
        model.addAttribute("todoTasks", allTasks.stream().filter(t -> "TODO".equals(t.getStatus())).toList());
        model.addAttribute("progressTasks", allTasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).toList());
        model.addAttribute("reviewTasks", allTasks.stream().filter(t -> "CODE_REVIEW".equals(t.getStatus())).toList());
        model.addAttribute("doneTasks", allTasks.stream()
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
                             java.security.Principal principal) {

        String username = principal.getName();
        User currentUser = userRepository.findByUsername(username);

        // for now taking the first project
        Project project = currentUser.getProjects().get(0);

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