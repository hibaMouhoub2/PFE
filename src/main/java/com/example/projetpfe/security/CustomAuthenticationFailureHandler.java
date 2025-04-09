package com.example.projetpfe.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String ip = getClientIP(request);
        logger.debug("Authentication failure for IP: {}", ip);

        // Mettre à jour le compteur d'échecs AVANT de vérifier si l'IP est bloquée
        loginAttemptService.loginFailed(ip);
        if (exception instanceof DisabledException) {
            logger.warn("Tentative de connexion sur un compte désactivé");
            getRedirectStrategy().sendRedirect(request, response, "/login?disabled=true");
            return;
        }

        // Maintenant vérifier si cette tentative a mené à un blocage
        if (loginAttemptService.isBlocked(ip)) {
            long remainingMinutes = loginAttemptService.getRemainingBlockTimeInMinutes(ip);
            logger.warn("IP {} bloquée après plusieurs échecs d'authentification. Temps restant: {} minutes",
                    ip, remainingMinutes);

            // Rediriger avec les informations de blocage
            getRedirectStrategy().sendRedirect(request, response,
                    "/login?blocked=true&remaining=" + remainingMinutes);
        } else {
            logger.debug("Échec d'authentification standard pour l'IP {}, redirection vers /login?error", ip);
            getRedirectStrategy().sendRedirect(request, response, "/login?error");
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        return xfHeader != null ? xfHeader.split(",")[0] : request.getRemoteAddr();
    }
}