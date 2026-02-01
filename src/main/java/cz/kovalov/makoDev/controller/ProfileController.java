package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.TaskRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository, TaskRepository taskRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public String myProfile(Model model, Principal principal) {
        User currentUser = userRepository.findByUsername(principal.getName());
        // redirect to universal method
        return viewUserProfile(currentUser.getId(), model, principal);
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam String email,
                                @RequestParam String bio,
                                Principal principal) {

        User user = userRepository.findByUsername(principal.getName());

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setBio(bio);

        userRepository.save(user);

        return "redirect:/profile";
    }

    @GetMapping("/profile/{id}")
    public String viewUserProfile(@PathVariable Long id, Model model, Principal principal) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String currentUsername = principal.getName();
        User currentUser = userRepository.findByUsername(currentUsername);

        int tasksDone = taskRepository.countByAssigneeAndStatus(targetUser, "DONE");
        int totalKudos = taskRepository.getTotalKudosForUser(targetUser);
        int currentProgress = targetUser.getXp() % 100;

        //whose profile is it
        boolean isOwner = targetUser.getId().equals(currentUser.getId());

        List<Task> allDoneTasks = taskRepository.findByAssigneeAndStatusOrderByIdDesc(targetUser, "DONE");
        List<Task> displayTasks;

        if (isOwner) {
            displayTasks = allDoneTasks;
        } else {
            displayTasks = allDoneTasks.stream().limit(3).toList();
        }

        model.addAttribute("user", targetUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isOwner", isOwner);

        model.addAttribute("tasksDone", tasksDone);
        model.addAttribute("totalKudos", totalKudos);
        model.addAttribute("currentProgress", currentProgress);

        model.addAttribute("completedTasks", displayTasks);

        return "profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Principal principal) {

        User user = userRepository.findByUsername(principal.getName());

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return "redirect:/profile?passError=Current password is incorrect";
        }

        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/profile?passError=New passwords do not match";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return "redirect:/profile?passSuccess";
    }
}