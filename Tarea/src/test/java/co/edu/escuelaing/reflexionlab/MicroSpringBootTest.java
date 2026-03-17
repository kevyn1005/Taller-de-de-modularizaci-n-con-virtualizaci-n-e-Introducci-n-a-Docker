package co.edu.escuelaing.reflexionlab;

import co.edu.escuelaing.reflexionlab.annotations.GetMapping;
import co.edu.escuelaing.reflexionlab.annotations.RequestParam;
import co.edu.escuelaing.reflexionlab.annotations.RestController;
import co.edu.escuelaing.reflexionlab.controllers.GreetingController;
import co.edu.escuelaing.reflexionlab.controllers.HelloController;
import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

public class MicroSpringBootTest {

    @Test
    public void testRestControllerAnnotationPresent() {
        assertTrue("HelloController debe tener @RestController",
                HelloController.class.isAnnotationPresent(RestController.class));
        assertTrue("GreetingController debe tener @RestController",
                GreetingController.class.isAnnotationPresent(RestController.class));
    }

    @Test
    public void testGetMappingAnnotationOnMethods() throws NoSuchMethodException {
        Method indexMethod = HelloController.class.getMethod("index");
        assertTrue("index() debe tener @GetMapping",
                indexMethod.isAnnotationPresent(GetMapping.class));
        assertEquals("index() debe mapearse a /",
                "/", indexMethod.getAnnotation(GetMapping.class).value());

        Method greetingMethod = GreetingController.class.getMethod("greeting", String.class);
        assertTrue("greeting() debe tener @GetMapping",
                greetingMethod.isAnnotationPresent(GetMapping.class));
        assertEquals("greeting() debe mapearse a /greeting",
                "/greeting", greetingMethod.getAnnotation(GetMapping.class).value());
    }

    @Test
    public void testRequestParamAnnotation() throws NoSuchMethodException {
        Method greetingMethod = GreetingController.class.getMethod("greeting", String.class);
        java.lang.reflect.Parameter param = greetingMethod.getParameters()[0];
        assertTrue("El parametro debe tener @RequestParam",
                param.isAnnotationPresent(RequestParam.class));
        assertEquals("El valor debe ser 'name'",
                "name", param.getAnnotation(RequestParam.class).value());
        assertEquals("El defaultValue debe ser 'World'",
                "World", param.getAnnotation(RequestParam.class).defaultValue());
    }

    @Test
    public void testControllerMethodsReturnCorrectValues() throws Exception {
        HelloController hello = new HelloController();
        assertEquals("Greetings from MicroSpringBoot!", hello.index());

        GreetingController greeting = new GreetingController();
        assertEquals("Hola Juan", greeting.greeting("Juan"));
        assertEquals("Hola World", greeting.greeting("World"));
    }

    @Test
    public void testPiEndpoint() throws Exception {
        GreetingController controller = new GreetingController();
        assertTrue("pi() debe contener el valor de PI",
                controller.pi().contains(String.valueOf(Math.PI)));
    }
}