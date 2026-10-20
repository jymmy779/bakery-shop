package com.bakery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(
            "/",
            "/api/products/**",
            "/products",
            "/cart/**",
            "/register/**",
            "/login",
            "/forgot-password/**",
            "/error",
            "/css/**",
            "/js/**",
            "/images/**",
            "/assets/**",
            "/uploads/**").permitAll()
        .requestMatchers("/checkout/**", "/orders/**", "/profile/**")
            .hasRole("CUSTOMER")
        .requestMatchers("/cms/accounts/**", "/cms/staff/**")
            .hasRole("ADMIN")
        .requestMatchers("/admin/**")
          .hasAnyRole("STAFF", "ADMIN")
        .requestMatchers("/cms/**")
            .hasAnyRole("STAFF", "ADMIN")
        .anyRequest().authenticated()
      )
      .formLogin(form -> form
        .loginPage("/login")
        .usernameParameter("email")
        .defaultSuccessUrl("/", false)
        .failureUrl("/login?error")
        .permitAll()
      )
      .logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessUrl("/login?logout")
        .invalidateHttpSession(true)
        .clearAuthentication(true)
        .permitAll()
      );

    return http.build();
  }
}
