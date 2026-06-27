package com.be.global.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {
            "/",
            "/home",
            "/create",
            "/share/{id}",
            "/vote/{id}",
            "/result/{id}",
            "/mypage"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
