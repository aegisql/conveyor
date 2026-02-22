package com.aegisql.conveyor.service.config;

import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class SecurityConfigTest {

    private static final String DEMO_AUTH_COOKIE = "CONVEYOR_DEMO_AUTH";
    private final SecurityConfig config = new SecurityConfig();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passwordEncoderAndDemoUsersAreConfigured() {
        var encoder = config.passwordEncoder();
        String encoded = encoder.encode("admin");
        assertThat(encoder.matches("admin", encoded)).isTrue();

        UserDetailsService users = config.demoUsers();
        assertThat(users.loadUserByUsername("rest").getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_REST_USER");
        assertThat(users.loadUserByUsername("viewer").getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_DASHBOARD_VIEWER");
        assertThat(users.loadUserByUsername("admin").getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_REST_USER", "ROLE_DASHBOARD_VIEWER", "ROLE_DASHBOARD_ADMIN");

        assertThat(config.webSecurityCustomizer()).isNotNull();
    }

    @Test
    void normalizeLinkedinAuthorizationRequestRemovesUnsupportedParameters() throws Exception {
        OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://www.linkedin.com/oauth/v2/authorization")
                .clientId("client")
                .redirectUri("http://localhost:8080/login/oauth2/code/linkedin")
                .scopes(Set.of("openid", "email"))
                .state("state")
                .additionalParameters(params -> {
                    params.put("prompt", "consent");
                    params.put("nonce", "nonce-value");
                    params.put("code_challenge", "challenge");
                    params.put("code_challenge_method", "S256");
                })
                .attributes(attrs -> {
                    attrs.put("registration_id", "linkedin");
                    attrs.put("nonce", "nonce-value");
                    attrs.put("code_verifier", "verifier");
                })
                .build();

        OAuth2AuthorizationRequest normalized = (OAuth2AuthorizationRequest) invokePrivateStatic(
                "normalizeLinkedinAuthorizationRequest",
                new Class<?>[]{OAuth2AuthorizationRequest.class},
                request
        );

        assertThat(normalized.getAdditionalParameters())
                .containsEntry("prompt", "consent")
                .doesNotContainKeys("nonce", "code_challenge", "code_challenge_method");
        assertThat(normalized.getAttributes())
                .containsEntry("registration_id", "linkedin")
                .doesNotContainKeys("nonce", "code_verifier");
        assertThat(normalized.getAuthorizationRequestUri()).doesNotContain("nonce", "code_challenge");
    }

    @Test
    void normalizeNonLinkedinAuthorizationRequestIsUnchanged() throws Exception {
        OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.example.com/auth")
                .clientId("client")
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .scopes(Set.of("openid"))
                .state("state")
                .attributes(attrs -> attrs.put("registration_id", "google"))
                .build();

        OAuth2AuthorizationRequest normalized = (OAuth2AuthorizationRequest) invokePrivateStatic(
                "normalizeLinkedinAuthorizationRequest",
                new Class<?>[]{OAuth2AuthorizationRequest.class},
                request
        );

        assertThat(normalized).isSameAs(request);
    }

    @Test
    void normalizeNullAuthorizationRequestReturnsNull() throws Exception {
        Object normalized = invokePrivateStatic(
                "normalizeLinkedinAuthorizationRequest",
                new Class<?>[]{OAuth2AuthorizationRequest.class},
                (Object) null
        );
        assertThat(normalized).isNull();
    }

    @Test
    void linkedinAuthorizationRequestResolverCoversBothResolveOverloads() throws Exception {
        ClientRegistration linkedin = clientRegistration("linkedin", "http://127.0.0.1/token");
        ClientRegistration google = clientRegistration("google", "http://127.0.0.1/token");
        ClientRegistrationRepository repository = new InMemoryClientRegistrationRepository(linkedin, google);

        OAuth2AuthorizationRequestResolver resolver = (OAuth2AuthorizationRequestResolver) invokePrivateStatic(
                "linkedinAuthorizationRequestResolver",
                new Class<?>[]{ClientRegistrationRepository.class},
                repository
        );

        MockHttpServletRequest byPath = new MockHttpServletRequest("GET", "/oauth2/authorization/linkedin");
        OAuth2AuthorizationRequest linkedinRequest = resolver.resolve(byPath);
        assertThat(linkedinRequest).isNotNull();
        assertThat(linkedinRequest.getAttributes()).containsEntry("registration_id", "linkedin");
        assertThat(linkedinRequest.getAttributes()).doesNotContainKeys("nonce", "code_verifier");
        assertThat(linkedinRequest.getAdditionalParameters()).doesNotContainKeys(
                "nonce", "code_challenge", "code_challenge_method"
        );

        MockHttpServletRequest byExplicitId = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        OAuth2AuthorizationRequest googleRequest = resolver.resolve(byExplicitId, "google");
        assertThat(googleRequest).isNotNull();
        assertThat(googleRequest.getAttributes()).containsEntry("registration_id", "google");
    }

    @Test
    @SuppressWarnings("unchecked")
    void linkedinTokenClientRemovesCodeVerifierOnlyForLinkedin() throws Exception {
        List<String> requestBodies = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger tokenCounter = new AtomicInteger();
        HttpServer tokenServer = HttpServer.create(new InetSocketAddress(0), 0);
        tokenServer.createContext("/token", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requestBodies.add(body);
            byte[] response = (
                    "{\"access_token\":\"token-" + tokenCounter.incrementAndGet()
                            + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}"
            ).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        tokenServer.start();

        try {
            String tokenUri = "http://127.0.0.1:" + tokenServer.getAddress().getPort() + "/token";
            ClientRegistration linkedin = clientRegistration("linkedin", tokenUri);
            ClientRegistration google = clientRegistration("google", tokenUri);
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient =
                    (OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>) invokePrivateStatic(
                            "linkedinAccessTokenResponseClient",
                            new Class<?>[]{}
                    );

            tokenClient.getTokenResponse(grantRequest(linkedin, "verifier-123"));
            tokenClient.getTokenResponse(grantRequest(google, "verifier-123"));

            assertThat(requestBodies).hasSize(2);
            assertThat(requestBodies.get(0)).doesNotContain("code_verifier=");
            assertThat(requestBodies.get(1)).contains("code_verifier=verifier-123");
        } finally {
            tokenServer.stop(0);
        }
    }

    @Test
    void demoCookieHelperMethodsValidateTokenAndCookieLifecycle() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        invokePrivateStatic(
                "writeDemoAuthCookie",
                new Class<?>[]{jakarta.servlet.http.HttpServletResponse.class, String.class},
                response,
                "admin"
        );
        Cookie issued = response.getCookie(DEMO_AUTH_COOKIE);
        assertThat(issued).isNotNull();

        String username = (String) invokePrivateStatic(
                "extractValidUsername",
                new Class<?>[]{String.class},
                issued.getValue()
        );
        assertThat(username).isEqualTo("admin");

        String expired = token("admin", Instant.now().minusSeconds(120).getEpochSecond());
        String badSignature = token("admin", Instant.now().plusSeconds(120).getEpochSecond()) + "broken";
        String malformed = Base64.getUrlEncoder().withoutPadding().encodeToString("bad-format".getBytes(StandardCharsets.UTF_8));

        assertThat(invokePrivateStatic("extractValidUsername", new Class<?>[]{String.class}, expired)).isNull();
        assertThat(invokePrivateStatic("extractValidUsername", new Class<?>[]{String.class}, badSignature)).isNull();
        assertThat(invokePrivateStatic("extractValidUsername", new Class<?>[]{String.class}, malformed)).isNull();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "x"), new Cookie(DEMO_AUTH_COOKIE, "token-value"));
        assertThat(invokePrivateStatic(
                "getCookieValue",
                new Class<?>[]{jakarta.servlet.http.HttpServletRequest.class, String.class},
                request,
                DEMO_AUTH_COOKIE
        )).isEqualTo("token-value");
        assertThat(invokePrivateStatic(
                "getCookieValue",
                new Class<?>[]{jakarta.servlet.http.HttpServletRequest.class, String.class},
                request,
                "missing"
        )).isNull();
        assertThat(invokePrivateStatic(
                "getCookieValue",
                new Class<?>[]{jakarta.servlet.http.HttpServletRequest.class, String.class},
                new MockHttpServletRequest(),
                DEMO_AUTH_COOKIE
        )).isNull();

        MockHttpServletResponse clearResponse = new MockHttpServletResponse();
        invokePrivateStatic(
                "clearDemoAuthCookie",
                new Class<?>[]{jakarta.servlet.http.HttpServletResponse.class},
                clearResponse
        );
        Cookie cleared = clearResponse.getCookie(DEMO_AUTH_COOKIE);
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();
    }

    @Test
    void demoCookieAuthFilterIgnoresMissingCookie() throws Exception {
        UserDetailsService users = mock(UserDetailsService.class);
        OncePerRequestFilter filter = newDemoCookieAuthFilter(users);

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        verifyNoInteractions(users);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void demoCookieAuthFilterAuthenticatesKnownUserFromCookie() throws Exception {
        UserDetailsService users = new InMemoryUserDetailsManager(
                User.withUsername("admin").password("{noop}admin").roles("DASHBOARD_ADMIN").build()
        );
        OncePerRequestFilter filter = newDemoCookieAuthFilter(users);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(DEMO_AUTH_COOKIE, token("admin", Instant.now().plusSeconds(120).getEpochSecond())));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin");
    }

    @Test
    void demoCookieAuthFilterClearsCookieWhenUserNotFound() throws Exception {
        UserDetailsService users = username -> {
            throw new IllegalArgumentException("missing user " + username);
        };
        OncePerRequestFilter filter = newDemoCookieAuthFilter(users);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(DEMO_AUTH_COOKIE, token("ghost", Instant.now().plusSeconds(120).getEpochSecond())));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        Cookie cleared = response.getCookie(DEMO_AUTH_COOKIE);
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void demoCookieAuthFilterSkipsLookupWhenSecurityContextAlreadyHasAuthentication() throws Exception {
        UserDetailsService users = mock(UserDetailsService.class);
        OncePerRequestFilter filter = newDemoCookieAuthFilter(users);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("already", "n/a", List.of())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(DEMO_AUTH_COOKIE, token("admin", Instant.now().plusSeconds(120).getEpochSecond())));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        verifyNoInteractions(users);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("already");
    }

    private static ClientRegistration clientRegistration(String registrationId, String tokenUri) {
        return ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-" + registrationId)
                .clientSecret("secret-" + registrationId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://example.com/oauth2/authorize")
                .tokenUri(tokenUri)
                .scope("email")
                .build();
    }

    private static OAuth2AuthorizationCodeGrantRequest grantRequest(
            ClientRegistration clientRegistration,
            String codeVerifier
    ) {
        String redirectUri = "http://localhost/login/oauth2/code/" + clientRegistration.getRegistrationId();
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                .clientId(clientRegistration.getClientId())
                .redirectUri(redirectUri)
                .state("state-" + clientRegistration.getRegistrationId())
                .attributes(attributes -> attributes.put(PkceParameterNames.CODE_VERIFIER, codeVerifier))
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success(
                        "code-" + clientRegistration.getRegistrationId())
                .state(authorizationRequest.getState())
                .redirectUri(redirectUri)
                .build();
        return new OAuth2AuthorizationCodeGrantRequest(
                clientRegistration,
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse)
        );
    }

    private static String token(String username, long expiresAt) throws Exception {
        String payload = username + ":" + expiresAt;
        String signature = (String) invokePrivateStatic("sign", new Class<?>[]{String.class}, payload);
        String value = payload + ":" + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static OncePerRequestFilter newDemoCookieAuthFilter(UserDetailsService users) throws Exception {
        Class<?> filterClass = null;
        for (Class<?> nested : SecurityConfig.class.getDeclaredClasses()) {
            if ("DemoCookieAuthFilter".equals(nested.getSimpleName())) {
                filterClass = nested;
                break;
            }
        }
        assertThat(filterClass).isNotNull();
        Constructor<?> ctor = filterClass.getDeclaredConstructor(UserDetailsService.class);
        ctor.setAccessible(true);
        return (OncePerRequestFilter) ctor.newInstance(users);
    }

    private static Object invokePrivateStatic(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = SecurityConfig.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }
}
