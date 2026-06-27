package com.be.global.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {
            "/",
            "/{path:^(?!api|oauth2|login|swagger-ui|api-docs|v3|assets|images|favicon\\.ico$).*$}",
            "/**/{path:^(?!api|oauth2|login|swagger-ui|api-docs|v3|assets|images|favicon\\.ico$).*$}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
