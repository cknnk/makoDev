package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.TaskRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public ProfileController(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @GetMapping("/profile")
    public String profilePage(Model model, Principal principal) {
        String username = principal.getName();
        User currentUser = userRepository.findByUsername(username);

        int tasksDone = taskRepository.countByAssigneeAndStatus(currentUser, "DONE");
        int totalKudos = taskRepository.getTotalKudosForUser(currentUser);

        int currentProgress = currentUser.getXp() % 100;

        model.addAttribute("user", currentUser);
        model.addAttribute("tasksDone", tasksDone);
        model.addAttribute("totalKudos", totalKudos);
        model.addAttribute("currentProgress", currentProgress);

        return "profile";
    }
}