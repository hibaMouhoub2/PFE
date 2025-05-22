package com.example.projetpfe.config;

import com.example.projetpfe.entity.Direction;
import com.example.projetpfe.entity.User;
import com.example.projetpfe.repository.DirectionRepository;
import com.example.projetpfe.repository.UserRepository;
import com.example.projetpfe.security.CustomAuthenticationFailureHandler;
import com.example.projetpfe.security.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SpringSecurity {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DirectionRepository directionRepository;

    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Bean
    public static PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests((authorize) ->
                        authorize.requestMatchers("/register/**").hasAnyRole("ADMIN","SUPER_ADMIN")
                                .requestMatchers("/index").permitAll()
                                .requestMatchers("/admin/directions/**").hasRole("SUPER_ADMIN")
                                .requestMatchers("/admin/directions").hasRole("SUPER_ADMIN")
                                .requestMatchers("/favicon.ico", "/css/**", "/js/**", "/images/**", "/webjars/**").hasAnyRole("ADMIN","SUPER_ADMIN","USER")
                                .requestMatchers("/login-success").permitAll()
                                .requestMatchers("/admin/regions/**").hasRole("SUPER_ADMIN")
                                .requestMatchers("/admin/regions").hasRole("SUPER_ADMIN")
                                .requestMatchers("/users-home").permitAll()
                                .requestMatchers("/api/branches").hasAnyRole("ADMIN", "SUPER_ADMIN")
                                .requestMatchers("/api/regions/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                                .requestMatchers("/users").hasAnyRole("ADMIN","SUPER_ADMIN")
                                .requestMatchers("/register-admin/**").hasAnyRole("SUPER_ADMIN")
                                .requestMatchers("/clients/**").hasAnyRole("USER","ADMIN")
                                .requestMatchers("/rappels/{id}/complete").hasAnyRole("USER", "ADMIN","SUPER_ADMIN")
                                .requestMatchers("/delete-user/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                                .requestMatchers("/edit-user/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                                .requestMatchers("/admin/reports/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                                .requestMatchers("/admin/**").hasAnyRole("ADMIN","SUPER_ADMIN")
                                .requestMatchers("/admin/reports/api/export/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                                .requestMatchers("/clients/*/questionnaire").access(
                                        (authentication, object) -> new AuthorizationDecision(
                                                authentication.get().getAuthorities().stream()
                                                        .anyMatch(a -> a.getAuthority().equals("ROLE_USER") || a.getAuthority().equals("ROLE_ADMIN"))
                                        )
                                )
                                .requestMatchers("/clients/**").authenticated()
                                .requestMatchers("/agenda/**").authenticated()
                                .requestMatchers("/admin/direction/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                                .requestMatchers("/admin/direction/{dirId}/**").access(
                                        (authentication, object) -> {
                                            Authentication auth = authentication.get();
                                            if (auth == null) return new AuthorizationDecision(false);

                                            User user = userRepository.findByEmail(auth.getName());

                                            // Récupérer l'ID de direction depuis l'URL
                                            HttpServletRequest request = object.getRequest();
                                            String path = request.getRequestURI();
                                            String dirIdStr = path.substring(path.indexOf("/direction/") + 11);
                                            dirIdStr = dirIdStr.contains("/") ? dirIdStr.substring(0, dirIdStr.indexOf("/")) : dirIdStr;

                                            try {
                                                Long dirId = Long.parseLong(dirIdStr);
                                                Direction direction = directionRepository.findById(dirId).orElse(null);

                                                return new AuthorizationDecision(
                                                        auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN")) ||
                                                                (direction != null && user.getDirection() != null &&
                                                                        user.getDirection().getId().equals(direction.getId()))
                                                );
                                            } catch (NumberFormatException e) {
                                                return new AuthorizationDecision(false);
                                            }
                                        }
                                )
                ).sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/login")
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired")
                )
                .formLogin(
                        form -> form
                                .loginPage("/login")
                                .loginProcessingUrl("/login")
                                .defaultSuccessUrl("/login-success", true)
                                .failureHandler(customAuthenticationFailureHandler)
                                .permitAll()
                ).logout(
                        logout -> logout
                                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                                .logoutSuccessUrl("/login?logout")
                                .deleteCookies("JSESSIONID")
                                .clearAuthentication(true)
                                .invalidateHttpSession(true)
                                .permitAll()
                );
        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }
    @Bean
    public SpringSecurityDialect securityDialect() {
        return new SpringSecurityDialect();
    }


}

