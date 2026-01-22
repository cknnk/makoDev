package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.ProjectRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectController(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/project/switch/{id}")
    public String switchProject(@PathVariable Long id, HttpSession session, Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        Project project = projectRepository.findById(id).orElseThrow();

        if (project.getMembers().contains(user)) {
            session.setAttribute("activeProjectId", id);
        }

        return "redirect:/";
    }

    @PostMapping("/project/create")
    public String createProject(@RequestParam String name,
                                @RequestParam String description,
                                Principal principal,
                                HttpSession session) {

        User currentUser = userRepository.findByUsername(principal.getName());

        Project newProject = new Project();
        newProject.setName(name);
        newProject.setDescription(description);
        newProject.setOwner(currentUser);
        newProject.setMembers(List.of(currentUser));

        Project savedProject = projectRepository.save(newProject);

        //switch user to new project rn
        session.setAttribute("activeProjectId", savedProject.getId());

        return "redirect:/";
    }
}