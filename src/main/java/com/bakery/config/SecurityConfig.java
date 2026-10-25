package com.bakery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
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
            "/products",
            "/products/**",
            "/about",
            "/contact",
            "/api/products/**",
            "/cart/**",
            "/register/**",
            "/login",
            "/cms/login",
            "/forgot-password/**",
            "/error",
            "/css/**",
            "/js/**",
            "/images/**",
            "/products/**",
            "/assets/**",
            "/uploads/**").permitAll()
        .requestMatchers("/checkout/**", "/orders/**", "/profile/**")
            .hasRole("CUSTOMER")
        .requestMatchers("/cms/accounts/**", "/cms/staff/**")
            .hasRole("ADMIN")
        .requestMatchers("/cms/**")
            .hasAnyRole("STAFF", "ADMIN")
        .anyRequest().authenticated()
      )
      .exceptionHandling(exceptions -> exceptions
        .authenticationEntryPoint((request, response, exception) -> {
          String loginPage = request.getRequestURI().startsWith("/cms/")
              ? "/cms/login"
              : "/login";
          response.sendRedirect(request.getContextPath() + loginPage);
        })
      )
      .formLogin(form -> form
        .loginPage("/login")
        .usernameParameter("email")
        .successHandler((request, response, authentication) -> {
          boolean cmsLogin = "cms".equals(request.getParameter("loginType"));
          boolean customer = authentication.getAuthorities().stream()
              .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER"));
          boolean cmsUser = authentication.getAuthorities().stream()
              .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF")
                  || authority.getAuthority().equals("ROLE_ADMIN"));

          if ((cmsLogin && !cmsUser) || (!cmsLogin && !customer)) {
            SecurityContextHolder.clearContext();
            if (request.getSession(false) != null) {
              request.getSession(false).invalidate();
            }
            response.sendRedirect(cmsLogin ? "/cms/login?roleError" : "/login?roleError");
            return;
          }

          response.sendRedirect(cmsLogin ? "/cms" : "/profile");
        })
        .failureHandler((request, response, exception) -> {
          String failureUrl = "cms".equals(request.getParameter("loginType"))
              ? "/cms/login?error"
              : "/login?error";
          response.sendRedirect(failureUrl);
        })
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
