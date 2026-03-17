package co.edu.escuelaing.reflexionlab.controllers;

import co.edu.escuelaing.reflexionlab.annotations.GetMapping;
import co.edu.escuelaing.reflexionlab.annotations.RequestParam;
import co.edu.escuelaing.reflexionlab.annotations.RestController;

import java.util.concurrent.atomic.AtomicLong;

@RestController
public class GreetingController {

    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return "Hola " + name;
    }

    @GetMapping("/pi")
    public String pi() {
        return "El valor de PI es: " + Math.PI;
    }

    @GetMapping("/count")
    public String count() {
        return "Visitas: " + counter.incrementAndGet();
    }
}