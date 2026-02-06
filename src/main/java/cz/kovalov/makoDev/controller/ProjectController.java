package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.service.ProjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/project/switch/{id}")
    public String switchProject(@PathVariable Long id, HttpSession session, Principal principal) {
        projectService.switchProject(id, principal.getName(), session);
        return "redirect:/";
    }

    @PostMapping("/project/create")
    public String createProject(@RequestParam String name,
                                @RequestParam String description,
                                Principal principal,
                                HttpSession session) {
        projectService.createProject(name, description, principal.getName(), session);
        return "redirect:/";
    }

    @PostMapping("/project/add-member")
    public String addMember(@RequestParam Long projectId,
                            @RequestParam String username,
                            Principal principal) {

        String error = projectService.addMember(projectId, username, principal.getName());

        if (error != null) {
            return "redirect:/?error=" + error;
        }
        return "redirect:/?success=Member added";
    }

    @PostMapping("/project/remove-member")
    public String removeMember(@RequestParam Long projectId,
                               @RequestParam Long memberId,
                               Principal principal) {

        String error = projectService.removeMember(projectId, memberId, principal.getName());

        if (error != null) {
            return "redirect:/?error=" + error;
        }
        return "redirect:/?success=Member removed";
    }

    @PostMapping("/project/update-info")
    public String updateProjectInfo(@RequestParam Long projectId,
                                    @RequestParam String description,
                                    Principal principal) {

        String error = projectService.updateProjectInfo(projectId, description, principal.getName());

        if (error != null) {
            return "redirect:/?error=" + error;
        }
        return "redirect:/?success=Project info updated";
    }

    @PostMapping("/project/delete")
    public String deleteProject(@RequestParam Long projectId,
                                Principal principal,
                                HttpSession session) {

        String error = projectService.deleteProject(projectId, principal.getName(), session);

        if (error != null) {
            return "redirect:/?error=" + error;
        }
        return "redirect:/?success=Project deleted";
    }
}