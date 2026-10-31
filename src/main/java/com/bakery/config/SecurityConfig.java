package com.bakery.config;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

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
            "/api/categories/**",
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
        .requestMatchers("/cms/login").permitAll()
        .requestMatchers("/cms/accounts/**", "/cms/staff/**")
            .hasRole("ADMIN")
        .requestMatchers("/cms/**")
            .hasAnyRole("STAFF", "ADMIN")
        .anyRequest().authenticated()
      )
      .exceptionHandling(exceptions -> exceptions
        .authenticationEntryPoint((request, response, exception) -> {
          response.sendRedirect(request.getContextPath() + "/login");
        })
      )
      .formLogin(form -> form
        .loginPage("/login")
        .loginProcessingUrl("/login")
        .usernameParameter("email")
        .successHandler((request, response, authentication) -> {
          boolean customer = authentication.getAuthorities().stream()
              .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER"));
          boolean cmsUser = authentication.getAuthorities().stream()
              .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF")
                  || authority.getAuthority().equals("ROLE_ADMIN"));

          if (!customer && !cmsUser) {
            response.sendRedirect("/login?roleError");
            return;
          }

          HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
          SavedRequest savedRequest = requestCache.getRequest(request, response);
          String fallbackUrl = customer ? "/profile" : "/cms";
          String targetUrl = customer ? savedCustomerTarget(savedRequest) : null;
          requestCache.removeRequest(request, response);
          response.sendRedirect(targetUrl != null ? targetUrl : fallbackUrl);
        })
        .failureHandler((request, response, exception) -> {
          response.sendRedirect("/login?error");
        })
        .permitAll()
      )
      .logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessHandler((request, response, authentication) -> {
          response.sendRedirect(request.getContextPath() + "/login?logout");
        })
        .invalidateHttpSession(true)
        .clearAuthentication(true)
        .permitAll()
      );

    return http.build();
  }

  private String savedCustomerTarget(SavedRequest savedRequest) {
    if (savedRequest == null || !"GET".equalsIgnoreCase(savedRequest.getMethod())) {
      return null;
    }

    try {
      URI savedUri = URI.create(savedRequest.getRedirectUrl());
      String path = savedUri.getRawPath();
      if (path == null || !path.startsWith("/")) {
        return null;
      }

      boolean cmsPath = path.equals("/cms") || path.startsWith("/cms/");
      if (cmsPath) {
        return null;
      }

      String query = sanitizedQuery(savedUri.getRawQuery());
      return query == null ? path : path + "?" + query;
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private String sanitizedQuery(String rawQuery) {
    if (rawQuery == null) {
      return null;
    }

    String query = Arrays.stream(rawQuery.split("&"))
        .filter(parameter -> !parameter.equals("continue")
            && !parameter.startsWith("continue="))
        .collect(Collectors.joining("&"));
    return query.isBlank() ? null : query;
  }
}
