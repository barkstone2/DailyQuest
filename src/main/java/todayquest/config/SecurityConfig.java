package todayquest.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import todayquest.jwt.JwtAuthorizationFilter;
import todayquest.jwt.JwtTokenProvider;
import todayquest.properties.SecurityOriginProperties;
import todayquest.properties.SecurityUrlProperties;
import todayquest.user.dto.RoleType;

import java.util.List;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthorizationFilter jwtAuthorizationFilter;
    private final SecurityUrlProperties securityUrlProperties;
    private final SecurityOriginProperties securityOriginProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().cors();

        http.authorizeHttpRequests()
                .requestMatchers(securityUrlProperties.getAllowedUrl()).permitAll()
                .requestMatchers(securityUrlProperties.getAdminUrl()).hasAuthority(RoleType.ADMIN.getCode())
                .anyRequest().authenticated();

        http.logout()
                .logoutUrl(securityUrlProperties.getLogoutUrl())
                .deleteCookies("JSESSIONID")
                .deleteCookies(JwtTokenProvider.ACCESS_TOKEN_NAME)
                .deleteCookies(JwtTokenProvider.REFRESH_TOKEN_NAME);

        http.addFilterBefore(jwtAuthorizationFilter, BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(securityOriginProperties.getAllowedOrigin());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
