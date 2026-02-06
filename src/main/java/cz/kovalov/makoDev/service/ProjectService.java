package cz.kovalov.makoDev.service;

import cz.kovalov.makoDev.data.entity.Project;
import cz.kovalov.makoDev.data.entity.Task;
import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.ProjectRepository;
import cz.kovalov.makoDev.data.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    private static final int REVIEW_REWARD = 15;

    public ProjectService(UserRepository userRepository, ProjectRepository projectRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    public Project getActiveProject(User user, HttpSession session) {
        Project activeProject = null;

        // 1. session
        Long sessionProjectId = (Long) session.getAttribute("activeProjectId");
        if (sessionProjectId != null) {
            activeProject = user.getProjects().stream()
                    .filter(p -> p.getId().equals(sessionProjectId))
                    .findFirst().orElse(null);
        }

        // 2. database
        if (activeProject == null && user.getCurrentProject() != null) {
            if (user.getProjects().contains(user.getCurrentProject())) {
                activeProject = user.getCurrentProject();
                session.setAttribute("activeProjectId", activeProject.getId());
            }
        }

        // 3. any available one
        if (activeProject == null && !user.getProjects().isEmpty()) {
            activeProject = user.getProjects().get(0);

            session.setAttribute("activeProjectId", activeProject.getId());
            user.setCurrentProject(activeProject);
            userRepository.save(user);
        }

        // return null if project wasn't found
        return activeProject;
    }

    @Transactional
    public void switchProject(Long projectId, String username, HttpSession session) {
        User user = userRepository.findByUsername(username);
        Project project = projectRepository.findById(projectId).orElseThrow();

        if (project.getMembers().contains(user)) {
            session.setAttribute("activeProjectId", projectId);
            user.setCurrentProject(project);
            userRepository.save(user);
        }
    }

    @Transactional
    public void createProject(String name, String description, String username, HttpSession session) {
        User currentUser = userRepository.findByUsername(username);

        Project newProject = new Project();
        newProject.setName(name);
        newProject.setDescription(description);
        newProject.setOwner(currentUser);
        newProject.setMembers(List.of(currentUser));

        Project savedProject = projectRepository.save(newProject);

        //switch user to new project rn
        session.setAttribute("activeProjectId", savedProject.getId());
    }

    @Transactional
    public String addMember(Long projectId, String memberUsername, String ownerUsername) {
        User currentUser = userRepository.findByUsername(ownerUsername);
        Project project = projectRepository.findById(projectId).orElseThrow();

        if (!project.getOwner().equals(currentUser)) {
            return "Only owner can add members";
        }

        User newMember = userRepository.findByUsername(memberUsername);
        if (newMember == null) {
            return "User not found";
        }

        if (project.getMembers().contains(newMember)) {
            return "User already in project";
        }

        project.getMembers().add(newMember);
        projectRepository.save(project);
        return null;
    }

    @Transactional
    public String removeMember(Long projectId, Long memberId, String ownerUsername) {
        User currentUser = userRepository.findByUsername(ownerUsername);
        Project project = projectRepository.findById(projectId).orElseThrow();

        if (!project.getOwner().equals(currentUser)) {
            return "Access denied";
        }

        if (memberId.equals(project.getOwner().getId())) {
            return "Cannot remove owner";
        }

        project.getMembers().removeIf(user -> user.getId().equals(memberId));
        projectRepository.save(project);
        return null;
    }

    @Transactional
    public String updateProjectInfo(Long projectId, String description, String ownerUsername) {
        User currentUser = userRepository.findByUsername(ownerUsername);
        Project project = projectRepository.findById(projectId).orElseThrow();

        if (!project.getOwner().equals(currentUser)) {
            return "Only owner can edit project info";
        }

        project.setDescription(description);
        projectRepository.save(project);
        return null;
    }

    @Transactional
    public String deleteProject(Long projectId, String ownerUsername, HttpSession session) {
        User currentUser = userRepository.findByUsername(ownerUsername);
        Project project = projectRepository.findById(projectId).orElseThrow();

        if (!project.getOwner().equals(currentUser)) {
            return "Only owner can delete project";
        }

        for (User member : project.getMembers()) {
            if (member.getCurrentProject() != null && member.getCurrentProject().getId().equals(projectId)) {
                member.setCurrentProject(null);
                userRepository.save(member);
            }
        }

        // Очистка сессии
        Long sessionProjectId = (Long) session.getAttribute("activeProjectId");
        if (sessionProjectId != null && sessionProjectId.equals(projectId)) {
            session.removeAttribute("activeProjectId");
        }

        project.getMembers().clear();
        projectRepository.save(project);
        projectRepository.delete(project);

        return null;
    }

    public int calculateProjectTotalXp(List<Task> projectTasks) {
        if (projectTasks == null || projectTasks.isEmpty()) {
            return 0;
        }

        // tasks
        int tasksXp = projectTasks.stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .mapToInt(Task::getRewardXp)
                .sum();

        // review
        int reviewXp = projectTasks.stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .filter(t -> t.getReviewer() != null)
                .mapToInt(t -> REVIEW_REWARD)
                .sum();

        // kudos
        int kudosXp = projectTasks.stream()
                .mapToInt(t -> t.getKudosCount() * 5)
                .sum();

        return tasksXp + reviewXp + kudosXp;
    }

    public int calculateLevel(int totalXp) {
        return 1 + (totalXp / 1000);
    }

    public int calculateProgress(int totalXp) {
        return totalXp % 1000;
    }

    public int calculateUserContributionXp(List<Task> allTasks, User user) {
        // tasks
        int myTaskRewardXp = allTasks.stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(user.getId()))
                .mapToInt(Task::getRewardXp)
                .sum();

        // kudos
        int myKudosXp = allTasks.stream()
                .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(user.getId()))
                .mapToInt(t -> t.getKudosCount() * 5)
                .sum();

        // review
        int myReviewXp = (int) allTasks.stream()
                .filter(t -> "DONE".equals(t.getStatus()))
                .filter(t -> t.getReviewer() != null && t.getReviewer().getId().equals(user.getId()))
                .count() * REVIEW_REWARD; // REVIEW_REWARD = 15 (у тебя есть эта константа в сервисе)

        return myTaskRewardXp + myKudosXp + myReviewXp;
    }
}