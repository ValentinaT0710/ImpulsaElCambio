package com.impulsaElCambio.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class indexController {

    @GetMapping("/home")
    public String home() {
        return "index";
    }
}
