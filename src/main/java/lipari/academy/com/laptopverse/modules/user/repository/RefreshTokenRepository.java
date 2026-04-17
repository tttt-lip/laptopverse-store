package lipari.academy.com.laptopverse.modules.user.repository;

import lipari.academy.com.laptopverse.modules.auth.dto.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(Long userId);

}
