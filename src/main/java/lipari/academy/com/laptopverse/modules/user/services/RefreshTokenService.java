package lipari.academy.com.laptopverse.modules.user.services;

import lipari.academy.com.laptopverse.modules.auth.dto.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenService extends JpaRepository<RefreshToken, Long> {
}
