package com.darshan.caption.aicaptiongenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    @GetMapping("/")
    public String root() {
        // This log message is our test.
        logger.info("PageController's root method was called. This is correct.");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            logger.info("User is authenticated. Redirecting to /generator.");
            return "redirect:/generator";
        } else {
            logger.info("User is not authenticated. Redirecting to /login.");
            return "redirect:/login";
        }
    }

    // ... other methods (/login, /signup, etc.) remain the same
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping("/generator")
    public String generator() {
        return "index";
    }

    @GetMapping("/history")
    public String history() {
        return "history";
    }
}

