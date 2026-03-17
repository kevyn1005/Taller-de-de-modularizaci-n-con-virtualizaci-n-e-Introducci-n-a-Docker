package co.edu.escuelaing.reflexionlab;

import co.edu.escuelaing.reflexionlab.annotations.RestController;
import co.edu.escuelaing.reflexionlab.server.HttpServer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MicroSpringBoot {

    public static void main(String[] args) throws Exception {
        HttpServer server = new HttpServer();

        if (args.length > 0) {
            System.out.println("[MicroSpring] Loading class: " + args[0]);
            loadSingleClass(args[0], server);
        } else {
            System.out.println("[MicroSpring] Scanning classpath for @RestController...");
            scanAndLoadControllers(server);
        }

        server.start();
    }

    private static void loadSingleClass(String className, HttpServer server) throws Exception {
        Class<?> clazz = Class.forName(className);
        Object instance = clazz.getDeclaredConstructor().newInstance();
        server.registerController(instance);
    }

    private static void scanAndLoadControllers(HttpServer server) throws Exception {
        List<Class<?>> controllers = findAnnotatedClasses(RestController.class);

        if (controllers.isEmpty()) {
            System.out.println("[MicroSpring] No @RestController classes found.");
            return;
        }

        for (Class<?> clazz : controllers) {
            System.out.println("[MicroSpring] Found: " + clazz.getName());
            Object instance = clazz.getDeclaredConstructor().newInstance();
            server.registerController(instance);
        }
    }

    private static List<Class<?>> findAnnotatedClasses(Class<? extends java.lang.annotation.Annotation> annotation) throws Exception {
        List<Class<?>> result = new ArrayList<>();
        String classpath = System.getProperty("java.class.path");
        String[] entries = classpath.split(File.pathSeparator);

        for (String entry : entries) {
            File file = new File(entry);
            if (file.isDirectory()) {
                scanDirectory(file, file, annotation, result);
            }
        }
        return result;
    }

    private static void scanDirectory(File rootDir, File currentDir,
                                      Class<? extends java.lang.annotation.Annotation> annotation, List<Class<?>> result) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(rootDir, file, annotation, result);
            } else if (file.getName().endsWith(".class")) {
                String className = getClassName(rootDir, file);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(annotation)) {
                        result.add(clazz);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // skip
                }
            }
        }
    }

    private static String getClassName(File rootDir, File classFile) {
        String rootPath = rootDir.getAbsolutePath();
        String filePath = classFile.getAbsolutePath();
        String relative = filePath.substring(rootPath.length() + 1);
        return relative.replace(File.separatorChar, '.').replace(".class", "");
    }
}