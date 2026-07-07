package com.shopease;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopease.dto.AuthDtos.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test void userCanRegisterAndLogin() throws Exception {
        RegisterRequest registration = new RegisterRequest("Asha", "asha@example.com", "Password1");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(registration)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("USER"));

        LoginRequest login = new LoginRequest("asha@example.com", "Password1");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(login)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test void productsArePublicButAdminWritesAreProtected() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/products/1")).andExpect(status().isForbidden());
    }
}
