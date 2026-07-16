package com.scraper.platform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocViewerController {

    @GetMapping("/docs/view")
    public String viewer() {
        return "docs/viewer";
    }
}
