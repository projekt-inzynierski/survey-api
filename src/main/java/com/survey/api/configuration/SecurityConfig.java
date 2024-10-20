package com.survey.api.configuration;

import com.survey.api.security.JwtAuthEntryPoint;
import com.survey.api.security.JwtAuthenticationFilter;
import com.survey.application.services.UserDetailsServiceImpl;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthEntryPoint authEntryPoint;
    private final UserDetailsServiceImpl userDetailsService;

    @Autowired
    public SecurityConfig(JwtAuthEntryPoint authEntryPoint, UserDetailsServiceImpl userDetailsService) {
        this.authEntryPoint = authEntryPoint;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> {
                    e.authenticationEntryPoint(authEntryPoint);
                })
                .authorizeHttpRequests(r -> {
                    r.requestMatchers(HttpMethod.POST,"/api/authentication/login").permitAll();
                    r.requestMatchers(HttpMethod.POST, "/api/authentication/respondents").permitAll();
                    r.requestMatchers(HttpMethod.POST, "/api/surveys").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/greeneryareacategories").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/occupationcategories").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/lifesatisfaction").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/agecategories").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/stresslevels").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/healthconditions").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/educationcategories").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/qualityofsleep").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/medicationuse").permitAll();
                    r.requestMatchers(HttpMethod.POST, "/api/respondents").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/respondents").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/respondents/all").permitAll();
                    r.requestMatchers(HttpMethod.POST, "/api/surveysendingpolicies").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/surveys").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/surveys/short").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/respondentgroups").permitAll();
                    r.requestMatchers(HttpMethod.POST, "/api/surveyresponses").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/surveysendingpolicies").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/surveys/shortsummaries").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/summaries/histogram").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/surveyresponses/results").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();
                    r.requestMatchers(HttpMethod.GET, "/api/surveys/allwithtimeslots").permitAll();
                })
                .httpBasic(Customizer.withDefaults());
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Filter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
}