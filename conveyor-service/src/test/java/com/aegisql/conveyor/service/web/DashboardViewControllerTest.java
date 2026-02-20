package com.aegisql.conveyor.service.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DashboardViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dashboardReturnsHtmlForViewer() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("viewer").roles("DASHBOARD_VIEWER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void dashboardShowsEmailForOAuthPrincipalWhenAvailable() throws Exception {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_DASHBOARD_VIEWER"));
        var oauthUser = new DefaultOAuth2User(
                authorities,
                Map.of(
                        "sub", "123456",
                        "email", "viewer@example.com",
                        "name", "Viewer Name"
                ),
                "sub"
        );
        var authenticationToken = new OAuth2AuthenticationToken(oauthUser, authorities, "facebook");

        mockMvc.perform(get("/dashboard").with(authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("viewer@example.com")));
    }

    @Test
    void dashboardShowsNameForOAuthPrincipalWhenEmailMissing() throws Exception {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_DASHBOARD_VIEWER"));
        var oauthUser = new DefaultOAuth2User(
                authorities,
                Map.of(
                        "sub", "654321",
                        "name", "Viewer Name"
                ),
                "sub"
        );
        var authenticationToken = new OAuth2AuthenticationToken(oauthUser, authorities, "linkedin");

        mockMvc.perform(get("/dashboard").with(authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Viewer Name")));
    }
}
