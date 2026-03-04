package com.aegisql.conveyor.service.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiListsRestOperationsWithExpectedResponseCodes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/part/{conveyor}/{id}/{label}'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/part/{conveyor}/{id}/{label}'].post.responses['202']").exists())
                .andExpect(jsonPath("$.paths['/command/{conveyor}/{id}/{command}'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/command/{conveyor}/{id}/{command}'].post.responses['202']").exists())
                .andExpect(jsonPath("$.paths['/static-part/{conveyor}/{label}'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/static-part/{conveyor}/{label}'].post.responses['202']").exists())
                .andExpect(jsonPath("$.paths['/api/dashboard/admin/reload/{name}'].post.responses['202']").exists())
                .andExpect(jsonPath("$.paths['/api/dashboard/admin/{name}'].delete.responses['202']").exists());
    }

    @Test
    void openApiExcludesServerRenderedDashboardEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/dashboard']").doesNotExist())
                .andExpect(jsonPath("$.paths['/dashboard/test/place']").doesNotExist())
                .andExpect(jsonPath("$.paths['/dashboard/test/static-part']").doesNotExist())
                .andExpect(jsonPath("$.paths['/dashboard/test/command']").doesNotExist());
    }
}

