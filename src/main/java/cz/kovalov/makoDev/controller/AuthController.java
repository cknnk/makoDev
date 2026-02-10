package cz.kovalov.makoDev.controller;

import cz.kovalov.makoDev.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    @Value("${app.invite.code}")
    private String correctInviteCode;

    public AuthController(UserService userService) {
        this.userService = userService;
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
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String inviteCode) {
        if (!correctInviteCode.equals(inviteCode)) {
            return "redirect:/register?error_code";
        }
        boolean isCreated = userService.registerUser(username, password);

        if (!isCreated) {
            return "redirect:/register?error_exists";
        }

        return "redirect:/login";
    }
}