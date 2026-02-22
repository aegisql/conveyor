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

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WatchController.class)
@Import({
        SecurityConfig.class,
        RestExceptionHandler.class,
        SecurityConfigProdDisabledWebMvcTest.TestBeans.class
})
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "conveyor.service.oauth2-login-enable=false",
        "conveyor.service.oauth2-resource-server-enable=false"
})
class SecurityConfigProdDisabledWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConveyorWatchService conveyorWatchService;

    @Test
    void unauthenticatedApiCallIsUnauthorizedWhenOauthLoginIsDisabled() throws Exception {
        mockMvc.perform(get("/api/dashboard/watch").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedDashboardViewerCanAccessWatchApi() throws Exception {
        when(conveyorWatchService.activeWatchesForUser("viewer"))
                .thenReturn(List.of(Map.of("watchId", "watch-1")));

        mockMvc.perform(get("/api/dashboard/watch")
                        .with(user("viewer").roles("DASHBOARD_VIEWER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].watchId").value("watch-1"));
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
