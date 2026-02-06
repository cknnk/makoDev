package cz.kovalov.makoDev.service;

import cz.kovalov.makoDev.data.entity.User;
import cz.kovalov.makoDev.data.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User getDashboardUser(String username) {
        User user = userRepository.findByUsername(username);

        if (user != null) {
            user.checkAndResetDailyStats();
            userRepository.save(user);
        }

        return user;
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional
    public void updateProfile(String username, String firstName, String lastName, String email, String bio) {
        User user = userRepository.findByUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setBio(bio);
        userRepository.save(user);
    }

    @Transactional
    public String changePassword(String username, String currentPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findByUsername(username);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return "Current password is incorrect";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "New passwords do not match";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return null;
    }

    @Transactional
    public boolean registerUser(String username, String password) {
        if (userRepository.findByUsername(username) != null) {
            return false;
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setXp(0);
        newUser.setLevel(1);
        newUser.setDailyXpEarned(0);
        newUser.setLastActiveDate(java.time.LocalDate.now());

        userRepository.save(newUser);
        return true;
    }
}