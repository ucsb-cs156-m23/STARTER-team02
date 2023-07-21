package edu.ucsb.cs156.example.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import edu.ucsb.cs156.example.entities.User;
import edu.ucsb.cs156.example.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  @Autowired
  UserRepository userRepository;

  @Value("${app.admin.emails}")
  private final List<String> adminEmails = new ArrayList<String>();

  // public static final String[] ENDPOINTS_WHITELIST = {
  //     "/",
  //     "/error",
  //     "/oauth2/authorization/google",
  //     "/swagger-ui/**",
  //     "/h2-console/**"
  // };


  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(authorize -> authorize
        .anyRequest().permitAll())
        // .requestMatchers(ENDPOINTS_WHITELIST).permitAll()
        // .anyRequest().authenticated())
        .exceptionHandling(handlingConfigurer -> handlingConfigurer
            .authenticationEntryPoint(new Http403ForbiddenEntryPoint()))
        .oauth2Login(
            oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(this.userAuthoritiesMapper())))
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
        .logout(logout -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
            .logoutSuccessUrl("/"));

    return http.build();

  }

  private GrantedAuthoritiesMapper userAuthoritiesMapper() {
    log.info("userAuthoritiesMapper called");
    return (authorities) -> {
      log.info("userAuthoritiesMapper called on authorities={}", authorities);

      Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

      authorities.forEach(authority -> {
        log.info("userAuthoritiesMapper called on authority={}", authority);

        mappedAuthorities.add(authority);
        if (OAuth2UserAuthority.class.isInstance(authority)) {
          OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority) authority;

          Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();

          String email = (String) userAttributes.get("email");
          if (getAdmin(email)) {
            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
          }

          if (email.endsWith("@ucsb.edu")) {
            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
          }
        }

      });
      log.info("mappedAuthorities={}", mappedAuthorities);
      return mappedAuthorities;
    };
  }

  public boolean getAdmin(String email) {
    if (adminEmails.contains(email)) {
      return true;
    }
    Optional<User> u = userRepository.findByEmail(email);
    return u.isPresent() && u.get().getAdmin();
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return (web) -> web.ignoring()
        .requestMatchers(new AntPathRequestMatcher("/h2-console/**"));
  }

}
