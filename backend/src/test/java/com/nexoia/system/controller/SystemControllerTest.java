package com.nexoia.system.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexoia.shared.exception.GlobalExceptionHandler;
import com.nexoia.shared.security.SecurityConfiguration;
import com.nexoia.shared.security.handler.ApiAccessDeniedHandler;
import com.nexoia.shared.security.handler.ApiAuthenticationEntryPoint;
import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.token.security.JwtCookieAuthenticationFilter;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import com.nexoia.system.dto.SystemResponse;
import com.nexoia.system.service.SystemService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = SystemController.class, properties =
        "nexo.security.token.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
        JwtCookieAuthenticationFilter.class})
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemService systemService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private TokenCookieService tokenCookieService;

    @MockitoBean
    private TokenSessionService tokenSessionService;

    @MockitoBean
    private ClientAccessService clientAccessService;

    @Test
    void exposesPublicSystemIdentityInsideTheStandardResponse() throws Exception {
        when(systemService.getSystemInformation()).thenReturn(new SystemResponse(
                "Nexo IA", "0.1.0-SNAPSHOT", "available", Instant.parse("2026-08-16T12:00:00Z")));

        mockMvc.perform(get("/api/v1/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("System information retrieved successfully"))
                .andExpect(jsonPath("$.data[0].name").value("Nexo IA"))
                .andExpect(jsonPath("$.data[0].status").value("available"))
                .andExpect(jsonPath("$.data[0].version").value("0.1.0-SNAPSHOT"))
                .andExpect(jsonPath("$.data[0].timestamp").value("2026-08-16T12:00:00Z"));
    }
}
