package lipari.academy.com.laptopverse.modules.auth.dto;

import jakarta.persistence.*;
import lipari.academy.com.laptopverse.modules.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 5000)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Column(nullable = false)
    private boolean isRevoked = false;

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    public void revoke() {
        this.isRevoked = true;
    }

    public boolean isActive() {
        return !isExpired() && !isRevoked;
    }
}
