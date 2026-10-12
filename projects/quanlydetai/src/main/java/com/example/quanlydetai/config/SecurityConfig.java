package com.example.quanlydetai.config;

import com.example.quanlydetai.security.AccountStatusFilter;
import com.example.quanlydetai.security.ApiSecurityResponse;
import com.example.quanlydetai.security.CustomUserDetailsService;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    @org.springframework.core.annotation.Order(1)
    SecurityFilterChain apiFilterChain(HttpSecurity http, CustomUserDetailsService users)
            throws Exception {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/api/v1/session/csrf")
                                        .permitAll()
                                        .requestMatchers("/api/v1/**")
                                        .authenticated()
                                        .anyRequest()
                                        .denyAll())
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(
                                                (request, response, ex) ->
                                                        ApiSecurityResponse.write(response, 401))
                                        .accessDeniedHandler(
                                                (request, response, ex) ->
                                                        ApiSecurityResponse.write(response, 403)))
                .requestCache(cache -> cache.disable())
                .addFilterBefore(new AccountStatusFilter(users), AuthorizationFilter.class);
        // Same session as form login. CSRF remains mandatory for every mutating API request.
        return http.build();
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    SecurityFilterChain filterChain(HttpSecurity http, CustomUserDetailsService users)
            throws Exception {
        http.authorizeHttpRequests(
                        auth ->
                                auth.dispatcherTypeMatchers(
                                                DispatcherType.ERROR, DispatcherType.FORWARD)
                                        .permitAll()
                                        .requestMatchers(
                                                "/login",
                                                "/css/**",
                                                "/js/**",
                                                "/images/hcmute-logo.png",
                                                "/images/hcmute-favicon.png",
                                                "/vendor/**",
                                                "/favicon.ico",
                                                "/actuator/health")
                                        .permitAll()
                                        .requestMatchers(
                                                org.springframework.http.HttpMethod.GET,
                                                "/sv/de-tai",
                                                "/de-tai",
                                                "/sv/nhom",
                                                "/sv/bao-cao",
                                                "/sv/ket-qua")
                                        .hasRole("SV")
                                        .requestMatchers(
                                                org.springframework.http.HttpMethod.GET,
                                                "/gv/de-tai",
                                                "/gv/bao-cao",
                                                "/gv/cham-diem")
                                        .hasRole("GV")
                                        .requestMatchers(
                                                org.springframework.http.HttpMethod.GET,
                                                "/truong-khoa/dot",
                                                "/truong-khoa/duyet",
                                                "/truong-khoa/hoi-dong")
                                        .hasRole("TRUONGKHOA")
                                        .requestMatchers(
                                                org.springframework.http.HttpMethod.GET,
                                                "/admin/tai-khoan")
                                        .hasRole("ADMIN")
                                        .requestMatchers(
                                                org.springframework.http.HttpMethod.GET,
                                                "/thong-bao",
                                                "/tai-khoan/mat-khau")
                                        .authenticated()
                                        .requestMatchers("/dean/**")
                                        .hasRole("TRUONGKHOA")
                                        .requestMatchers("/lecturer/**")
                                        .hasRole("GV")
                                        .requestMatchers("/student/**", "/topics")
                                        .hasRole("SV")
                                        .requestMatchers("/admin/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(
                                                "/",
                                                "/error",
                                                "/announcements",
                                                "/accounts/**",
                                                "/reports/**")
                                        .authenticated()
                                        .anyRequest()
                                        .denyAll())
                .formLogin(
                        form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(
                        logout ->
                                logout.logoutSuccessUrl("/login?logout")
                                        .invalidateHttpSession(true)
                                        .deleteCookies("JSESSIONID"))
                .sessionManagement(
                        session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .addFilterBefore(new AccountStatusFilter(users), AuthorizationFilter.class);
        // CSRF stays enabled; Thymeleaf inserts the token into POST forms.
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
