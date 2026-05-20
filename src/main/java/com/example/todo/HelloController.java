package com.example.todo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String home() {
        return "Spring Boot is running";
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello Spring Boot 1122";
    }

    @GetMapping("/api/hello")
    public String apiHello(){
        return "Hello From API";
    }
}
