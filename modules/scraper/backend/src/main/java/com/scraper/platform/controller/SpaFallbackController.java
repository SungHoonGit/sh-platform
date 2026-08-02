package com.scraper.platform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA 라우트 직접 접근/새로고침 시 index.html 로 포워딩한다.
 *
 * <p>프론트가 BrowserRouter basename=/scraper 로 동작하므로,
 * /scraper/viewer, /scraper/schedule 등으로 직접 진입해도 SPA 가 정상 로드되게 한다.</p>
 */
@Controller
public class SpaFallbackController {

    @GetMapping({"/search", "/schedule", "/viewer"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
