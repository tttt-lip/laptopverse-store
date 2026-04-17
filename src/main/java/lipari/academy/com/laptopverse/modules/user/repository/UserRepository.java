package lipari.academy.com.laptopverse.modules.user.repository;

import lipari.academy.com.laptopverse.modules.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsBydisplayName(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
