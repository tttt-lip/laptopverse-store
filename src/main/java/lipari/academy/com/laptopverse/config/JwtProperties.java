package lipari.academy.com.laptopverse.config;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private boolean httpOnly;
    private boolean cookieSecure;
    private String cookieSameSite;
    private Long timeExpiredAccessMs;
    private Long timeExpiredRefreshMs;

    public Long getTimeExpiredAccessSeconds() {
        return timeExpiredAccessMs == null ? 0L : timeExpiredAccessMs / 1000;
    }


    public Long getTimeExpiredRefreshSeconds() {
        return timeExpiredRefreshMs == null ? 0L : timeExpiredRefreshMs / 1000;
    }
}
