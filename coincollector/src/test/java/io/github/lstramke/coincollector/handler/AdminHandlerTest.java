package io.github.lstramke.coincollector.handler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lstramke.coincollector.security.SecurityConfig;
import io.github.lstramke.coincollector.services.SessionManager;

@WebMvcTest(AdminHandler.class)
@Import(SecurityConfig.class)
public class AdminHandlerTest {

    @MockitoBean
    ApplicationContext applicationContext;

    @MockitoBean
    SessionManager sessionManager;

    @Autowired
    private MockMvc mockMvc;


    @Test
    void shutdownEndpointReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/shutdown")).andExpect(status().isOk());
    }

}
