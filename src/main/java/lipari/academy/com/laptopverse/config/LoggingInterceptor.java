package lipari.academy.com.laptopverse.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. Estraiamo l'utente dal contesto di sicurezza
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) 
                ? auth.getName() 
                : "ANONYMOUS";

        // 2. Popoliamo l'MDC
        MDC.put("user", userEmail);
        MDC.put("uri", request.getRequestURI());
        MDC.put("method", request.getMethod());
        
        return true; // Continua l'esecuzione verso il Controller
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 3. Puliamo l'MDC alla fine di TUTTO (dopo che la vista è stata renderizzata o la risposta inviata)
        MDC.clear();
    }
}
