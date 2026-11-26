package org.example.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('CLIENT')")
public class ClientController {

    @GetMapping("/client/dashboard")
    public String clientDashboard() {
        return "client/dashboard";
    }
}
