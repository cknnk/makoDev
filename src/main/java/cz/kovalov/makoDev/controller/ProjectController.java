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

    @PostMapping("/project/add-member")
    public String addMember(@RequestParam Long projectId,
                            @RequestParam String username,
                            Principal principal) {

        User currentUser = userRepository.findByUsername(principal.getName());
        Project project = projectRepository.findById(projectId).orElseThrow();

        if (!project.getOwner().equals(currentUser)) {
            return "redirect:/?error=Only owner can add members";
        }

        User newMember = userRepository.findByUsername(username);
        if (newMember == null) {
            return "redirect:/?error=User not found";
        }

        if (project.getMembers().contains(newMember)) {
            return "redirect:/?error=User already in project";
        }

        project.getMembers().add(newMember);
        projectRepository.save(project);

        return "redirect:/?success=Member added";
    }

    @PostMapping("/project/remove-member")
    public String removeMember(@RequestParam Long projectId,
                               @RequestParam Long memberId,
                               Principal principal) {

        User currentUser = userRepository.findByUsername(principal.getName());
        Project project = projectRepository.findById(projectId).orElseThrow();

        if (!project.getOwner().equals(currentUser)) {
            return "redirect:/?error=Access denied";
        }

        if (memberId.equals(project.getOwner().getId())) {
            return "redirect:/?error=Cannot remove owner";
        }

        project.getMembers().removeIf(user -> user.getId().equals(memberId));
        projectRepository.save(project);

        return "redirect:/?success=Member removed";
    }

    @PostMapping("/project/update-info")
    public String updateProjectInfo(@RequestParam Long projectId,
                                    @RequestParam String description,
                                    Principal principal) {

        User currentUser = userRepository.findByUsername(principal.getName());
        Project project = projectRepository.findById(projectId).orElseThrow();

        if (!project.getOwner().equals(currentUser)) {
            return "redirect:/?error=Only owner can edit project info";
        }

        project.setDescription(description);
        projectRepository.save(project);

        return "redirect:/?success=Project info updated";
    }
}