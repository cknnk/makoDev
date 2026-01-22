package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.ProjectRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, ProjectRepository projectRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username, @RequestParam String password) {
        if (userRepository.findByUsername(username) != null) {
            return "redirect:/register?error";
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setXp(0);
        newUser.setLevel(1);
        newUser.setCoins(0);
        newUser.setDailyXpEarned(0);
        newUser.setLastActiveDate(LocalDate.now());

        userRepository.save(newUser);

        Project personalProject = new Project();
        personalProject.setName("My First Project");
        personalProject.setDescription("Personal workspace for " + username);
        personalProject.setMembers(List.of(newUser));

        personalProject.setOwner(newUser);

        projectRepository.save(personalProject);

        return "redirect:/login";
    }
}