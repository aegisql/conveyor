package com.aegisql.conveyor.service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String[] DASHBOARD_PATHS = {"/dashboard/**", "/api/dashboard/**"};
    private static final String[] DASHBOARD_ADMIN_PATHS = {"/dashboard/admin/**", "/api/dashboard/admin/**"};
    private static final String[] DASHBOARD_WS_PATHS = {"/ws/**"};
    private static final String[] SWAGGER_PATHS = {"/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml"};
    private static final int DEMO_REMEMBER_ME_SECONDS = 30 * 24 * 60 * 60;
    private static final String DEMO_REMEMBER_ME_COOKIE = "CONVEYOR_DEMO_REMEMBER_ME";
    private static final String DEMO_AUTH_COOKIE = "CONVEYOR_DEMO_AUTH";
    private static final String DEMO_AUTH_HMAC_KEY = "conveyor-demo-auth-cookie-signing-key";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @Profile("prod")
    SecurityFilterChain securityProd(
            HttpSecurity http,
            ConveyorServiceProperties properties,
            ClientRegistrationRepository clientRegistrationRepository
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SWAGGER_PATHS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/part/**", "/static-part/**", "/command/**").hasRole("REST_USER")
                        .requestMatchers(DASHBOARD_WS_PATHS).hasAnyRole("DASHBOARD_VIEWER", "DASHBOARD_ADMIN")
                        .requestMatchers(DASHBOARD_ADMIN_PATHS).hasRole("DASHBOARD_ADMIN")
                        .requestMatchers(DASHBOARD_PATHS).hasAnyRole("DASHBOARD_VIEWER", "DASHBOARD_ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        if (properties.isOauth2ResourceServerEnable()) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        } else {
            LOG.info("OAuth2 resource-server JWT validation is disabled (conveyor.service.oauth2-resource-server-enable=false)");
        }
        if (properties.isOauth2LoginEnable()) {
            http.oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(authorization -> authorization
                            .authorizationRequestResolver(
                                    linkedinAuthorizationRequestResolver(clientRegistrationRepository)
                            ))
                    .tokenEndpoint(token -> token
                            .accessTokenResponseClient(linkedinAccessTokenResponseClient()))
                    .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(authorities -> {
                        // LinkedIn/OIDC users do not carry app-specific roles by default.
                        // Grant dashboard viewer + REST user roles for authenticated UI usage.
                        Set<org.springframework.security.core.GrantedAuthority> mapped = new HashSet<>(authorities);
                        mapped.add(new SimpleGrantedAuthority("ROLE_DASHBOARD_VIEWER"));
                        mapped.add(new SimpleGrantedAuthority("ROLE_REST_USER"));
                        return mapped;
                    }))
                    .successHandler((request, response, authentication) ->
                            response.sendRedirect(request.getContextPath() + "/dashboard"))
                    .failureHandler((request, response, exception) -> {
                        LOG.error("OAuth2 login failed: {}", exception.getMessage(), exception);
                        response.sendRedirect(request.getContextPath() + "/login?error");
                    }));
        }
        return http.build();
    }

    private static OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> linkedinAccessTokenResponseClient() {
        RestClientAuthorizationCodeTokenResponseClient defaultClient = new RestClientAuthorizationCodeTokenResponseClient();
        RestClientAuthorizationCodeTokenResponseClient linkedinClient = new RestClientAuthorizationCodeTokenResponseClient();
        linkedinClient.setParametersCustomizer(parameters -> parameters.remove("code_verifier"));
        return grantRequest -> {
            if ("linkedin".equals(grantRequest.getClientRegistration().getRegistrationId())) {
                return linkedinClient.getTokenResponse(grantRequest);
            }
            return defaultClient.getTokenResponse(grantRequest);
        };
    }

    private static OAuth2AuthorizationRequestResolver linkedinAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        DefaultOAuth2AuthorizationRequestResolver delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
        );
        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return normalizeLinkedinAuthorizationRequest(delegate.resolve(request));
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                return normalizeLinkedinAuthorizationRequest(delegate.resolve(request, clientRegistrationId));
            }
        };
    }

    private static OAuth2AuthorizationRequest normalizeLinkedinAuthorizationRequest(OAuth2AuthorizationRequest request) {
        if (request == null) {
            return null;
        }
        if (!"linkedin".equals(request.getAttributes().get("registration_id"))) {
            return request;
        }
        Map<String, Object> additionalParameters = new LinkedHashMap<>(request.getAdditionalParameters());
        additionalParameters.remove("nonce");
        additionalParameters.remove("code_challenge");
        additionalParameters.remove("code_challenge_method");

        Map<String, Object> attributes = new LinkedHashMap<>(request.getAttributes());
        attributes.remove("nonce");
        attributes.remove("code_verifier");

        return OAuth2AuthorizationRequest.from(request)
                .additionalParameters(map -> {
                    map.clear();
                    map.putAll(additionalParameters);
                })
                .attributes(map -> {
                    map.clear();
                    map.putAll(attributes);
                })
                .authorizationRequestUri((String) null)
                .build();
    }

    @Bean
    @Profile("demo")
    SecurityFilterChain securityDemo(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SWAGGER_PATHS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/part/**", "/static-part/**", "/command/**").hasRole("REST_USER")
                        .requestMatchers(DASHBOARD_WS_PATHS).hasAnyRole("DASHBOARD_VIEWER", "DASHBOARD_ADMIN")
                        .requestMatchers(DASHBOARD_ADMIN_PATHS).hasRole("DASHBOARD_ADMIN")
                        .requestMatchers(DASHBOARD_PATHS).hasAnyRole("DASHBOARD_VIEWER", "DASHBOARD_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new DemoCookieAuthFilter(userDetailsService), UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.successHandler((request, response, authentication) -> {
                    writeDemoAuthCookie(response, authentication.getName());
                    response.sendRedirect(request.getContextPath() + "/dashboard");
                }))
                .logout(logout -> logout.logoutSuccessHandler((request, response, authentication) -> {
                    clearDemoAuthCookie(response);
                    response.sendRedirect(request.getContextPath() + "/login?logout");
                }))
                .rememberMe(remember -> remember
                        .key("conveyor-demo-remember-me-key")
                        .rememberMeCookieName(DEMO_REMEMBER_ME_COOKIE)
                        .rememberMeParameter("remember-me")
                        .userDetailsService(userDetailsService)
                        .tokenValiditySeconds(DEMO_REMEMBER_ME_SECONDS)
                        .alwaysRemember(true)
                        .useSecureCookie(false)
                );
        return http.build();
    }

    @Bean
    @Profile("demo")
    UserDetailsService demoUsers() {
        var rest = User.withUsername("rest")
                .password("{noop}rest")
                .roles("REST_USER")
                .build();
        var viewer = User.withUsername("viewer")
                .password("{noop}viewer")
                .roles("DASHBOARD_VIEWER")
                .build();
        var admin = User.withUsername("admin")
                .password("{noop}admin")
                .roles("REST_USER", "DASHBOARD_VIEWER", "DASHBOARD_ADMIN")
                .build();
        return new InMemoryUserDetailsManager(rest, viewer, admin);
    }

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                "/css/**", "/js/**", "/images/**", "/webjars/**", "/actuator/health", "/swagger-ui/**"
        );
    }

    private static void writeDemoAuthCookie(HttpServletResponse response, String username) {
        long expiresAt = Instant.now().getEpochSecond() + DEMO_REMEMBER_ME_SECONDS;
        String payload = username + ":" + expiresAt;
        String signature = sign(payload);
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));
        Cookie cookie = new Cookie(DEMO_AUTH_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(DEMO_REMEMBER_ME_SECONDS);
        response.addCookie(cookie);
    }

    private static void clearDemoAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(DEMO_AUTH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private static String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(DEMO_AUTH_HMAC_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign demo auth cookie", e);
        }
    }

    private static String extractValidUsername(String token) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            String value = new String(decoded, StandardCharsets.UTF_8);
            String[] parts = value.split(":", 3);
            if (parts.length != 3) {
                return null;
            }
            String username = parts[0];
            long expiresAt = Long.parseLong(parts[1]);
            String signature = parts[2];
            if (expiresAt < Instant.now().getEpochSecond()) {
                return null;
            }
            String payload = username + ":" + expiresAt;
            String expected = sign(payload);
            if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            return username;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static final class DemoCookieAuthFilter extends OncePerRequestFilter {

        private final UserDetailsService userDetailsService;

        private DemoCookieAuthFilter(UserDetailsService userDetailsService) {
            this.userDetailsService = userDetailsService;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String token = getCookieValue(request, DEMO_AUTH_COOKIE);
                String username = token == null ? null : extractValidUsername(token);
                if (username != null) {
                    try {
                        UserDetails user = userDetailsService.loadUserByUsername(username);
                        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                                user, null, user.getAuthorities()
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } catch (Exception e) {
                        clearDemoAuthCookie(response);
                    }
                }
            }
            filterChain.doFilter(request, response);
        }
    }
}
