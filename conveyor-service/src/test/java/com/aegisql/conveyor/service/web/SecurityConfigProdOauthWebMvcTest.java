package com.aegisql.conveyor.service.web;

import com.aegisql.conveyor.service.config.SecurityConfig;
import com.aegisql.conveyor.service.core.ConveyorWatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WatchController.class)
@Import({
        SecurityConfig.class,
        RestExceptionHandler.class,
        SecurityConfigProdOauthWebMvcTest.TestBeans.class
})
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "conveyor.service.oauth2-login-enable=true",
        "conveyor.service.oauth2-resource-server-enable=true",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1/jwks"
})
class SecurityConfigProdOauthWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConveyorWatchService conveyorWatchService;

    @Test
    void linkedinAuthorizationRedirectOmitsNonceAndPkceForLinkedin() throws Exception {
        String location = mockMvc.perform(get("/oauth2/authorization/linkedin"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        assertThat(location).isNotBlank();
        assertThat(location).startsWith("https://www.linkedin.com/oauth/v2/authorization?");
        assertThat(location).doesNotContain("nonce=");
        assertThat(location).doesNotContain("code_challenge=");
        assertThat(location).doesNotContain("code_challenge_method=");
        assertThat(location).contains("scope=openid");
    }

    @Test
    void unauthenticatedJsonApiRequestUsesUnauthorizedEntryPoint() throws Exception {
        mockMvc.perform(get("/api/dashboard/watch").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(linkedinRegistration());
        }

        private static ClientRegistration linkedinRegistration() {
            return ClientRegistration.withRegistrationId("linkedin")
                    .clientId("client-id")
                    .clientSecret("client-secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "email")
                    .authorizationUri("https://www.linkedin.com/oauth/v2/authorization")
                    .tokenUri("https://www.linkedin.com/oauth/v2/accessToken")
                    .userInfoUri("https://api.linkedin.com/v2/userinfo")
                    .userNameAttributeName("sub")
                    .clientName("LinkedIn")
                    .build();
        }
    }
}
