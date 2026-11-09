package com.scraper.platform.controller;

import com.scraper.platform.ai.CompanyAnalysisResult;
import com.scraper.platform.ai.CompanyAnalysisService;
import com.scraper.platform.config.SecurityConfig;
import com.scraper.platform.service.CompanyNoteService;
import com.shplatform.common.security.JwtTokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CompanyNoteController.class,
        properties = "jwt.public-key=-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxkUEb22qrrg6+oR7RNfWGOCzfMLACJvYiY6QUaEjY9BWoPMlt2QNiB8Pqqt75rTAINtMc7LBFsNN3jL7WsLsxU0TH1QJaNhgTnSERijpTVPaH+iOozsUWqMyEpaqnVCwzm673XPWPYyexPbDL4bF5gI57Z99nQS24Dq76hk6ok384ut/j0CzUn7IKDf33ENvsIbQkp1ICwBc4iKKSr8TnfMCcSpScWdYWo4aU+h+hR1x7aNkX9Qx7g/nL4YBafiq7Ubw35MWTdohFlENXyK7VNS+YWHbi67JyMGvVKcAG/AtJwtWBTFLUYp4wzo5CDfGQBEV8/ujehVdb/TryAlY9wIDAQAB-----END PUBLIC KEY-----")
@Import(SecurityConfig.class)
class CompanyNoteControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CompanyNoteService noteService;
    @MockBean CompanyAnalysisService analysisService;
    @MockBean JwtTokenValidator jwtTokenValidator;
    @MockBean com.scraper.platform.config.ApiKeyFilter apiKeyFilter;

    @Test
    void analyze_requires_authentication() throws Exception {
        mockMvc.perform(post("/company-notes/5/analyze"))
                .andExpect(status().isUnauthorized());
    }
}
