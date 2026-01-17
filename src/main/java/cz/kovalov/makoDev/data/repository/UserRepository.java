package cz.kovalov.makoDev.data.repository;

import cz.kovalov.makoDev.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

//@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}