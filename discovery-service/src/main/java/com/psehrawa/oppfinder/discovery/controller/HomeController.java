package com.psehrawa.oppfinder.discovery.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("appName", "TechOpportunity Intelligence Platform");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("profile", "Development Mode");
        return "home";
    }
}