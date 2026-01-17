package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.TaskRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public DashboardController(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        // for test purposes
        // later will be used user that logged in
        User currentUser = userRepository.findAll().get(0);
        List<Task> tasks = taskRepository.findAll();

        // to html
        model.addAttribute("user", currentUser);
        model.addAttribute("tasks", tasks);

        return "index";
    }
}