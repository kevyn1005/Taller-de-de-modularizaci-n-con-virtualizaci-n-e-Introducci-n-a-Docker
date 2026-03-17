package co.edu.escuelaing.reflexionlab.controllers;

import co.edu.escuelaing.reflexionlab.annotations.GetMapping;
import co.edu.escuelaing.reflexionlab.annotations.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String index() {
        return "Greetings from MicroSpringBoot!";
    }

    @GetMapping("/hello")
    public String hello() {
        return "<html><body><h1>Hello World!</h1><p>MicroSpringBoot is running.</p></body></html>";
    }
}