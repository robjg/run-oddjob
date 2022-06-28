/*
 * Copyright (c) 2005, Rob Gordon.
 */
package org.oddjob.launch;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * By Default, launch Oddjob using a classloader with the following:
 * <ul>
 *   <li>Any jars in the lib directory.</li>
 *   <li>Any jars in the opt/lib directory.</li>
 *   <li>The opt/classes directory.</li>
 * </ul>
 *
 * @author Rob Gordon, Based on Ant.
 */
public class Launcher {

    public static final String ODDJOB_HOME_PROPERTY = "oddjob.home";

    public static final String ODDJOB_RUN_JAR_PROPERTY = "oddjob.run.jar";

    public static final String ODDJOB_MAIN_CLASS = "org.oddjob.Main";

    public static final String MAIN_METHOD = "main";

    public static final String STOP_METHOD = "stop";

    private static volatile AutoCloseable stop;

    /**
     * The class loader to find the main class in.
     */
    private final ClassLoader classLoader;

    /**
     * The name of the class that contains the main method.
     */
    private final String className;

    /**
     * The arguments to pass to main.
     */
    private final String[] args;

    public Launcher(ClassLoader classLoader, String className, String[] args) {
        Objects.requireNonNull(classLoader);
        Objects.requireNonNull(className);

        this.classLoader = classLoader;
        this.className = className;
        this.args = args;
    }

    public void launch() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            Class<?> mainClass = classLoader.loadClass(className);

            // use reflection because main is now in another
            // class loader space, so we can't get at it.
            Method main = mainClass.getMethod(MAIN_METHOD,
                    String[].class);

            Method stop;
            try {
                stop = mainClass.getMethod(STOP_METHOD);
            }
            catch (NoSuchMethodException e) {
                // ignore. stop method optional.
                stop = null;
            }

            if (stop == null) {
                Launcher.stop = () -> {
                    throw new UnsupportedOperationException("No stop method");
                };
            }
            else {
                Method finalStop = stop;
                Launcher.stop = () -> finalStop.invoke(null);
            }

            main.invoke(null, (Object) args);

        } finally {
            Thread.currentThread().setContextClassLoader(currentLoader);
        }
    }

    /**
     * Provides the Oddjob class loader.
     *
     * @param currentLoader The current classloader that becomes the parent
     * @param classpath     The classpath to add to the class loader.
     * @return A class loader.
     * @throws IOException If Failing to set canonical paths.
     */
    static ClassLoader getClassLoader(ClassLoader currentLoader, String[] classpath)
            throws IOException {

        File sourceJar = Locator.getClassSource(Launcher.class);
        File jarDir = sourceJar.getParentFile();

        System.setProperty(ODDJOB_HOME_PROPERTY, jarDir.getCanonicalPath());
        System.setProperty(ODDJOB_RUN_JAR_PROPERTY, sourceJar.getCanonicalPath());

        List<File> classPathList = new ArrayList<>();

        // add the source jar
        classPathList.add(sourceJar);

        // expand the classpath entries.
        for (String entry : classpath) {
            File[] entryFiles = new FileSpec(
                    new File(entry)).getFiles();
            classPathList.addAll(Arrays.asList(entryFiles));
        }

        // expand the lib directory
        File[] libFiles = new FileSpec(
                new File(new File(jarDir, "lib"), "*.jar")).getFiles();
        classPathList.addAll(Arrays.asList(libFiles));

        // add opt/classes
        classPathList.add(new File(jarDir, "opt/classes"));

        // expand the opt/lib directory
        File[] optFiles = new FileSpec(new File(
                new File(jarDir, "opt/lib"), "*.jar")).getFiles();
        classPathList.addAll(Arrays.asList(optFiles));

        // The full class path
        ClassPathHelper classPathHelper = new ClassPathHelper(
                classPathList.toArray(new File[0]));

        URL[] urls = classPathHelper.toURLs();
        classPathHelper.appendToJavaClassPath();
        final String classPath = classPathHelper.toString();

        return new URLClassLoader(urls, currentLoader) {
            @Override
            public String toString() {
                return "Oddjob Launcher ClassLoader: " +
                        classPath;
            }
        };
    }

    /**
     * Main method for launching Oddjob in it's own class loader.
     * The parent class loader will be taken to be the current threads
     * context class loader.
     *
     * @param args Args passed to oddjob.
     */
    public static void main(String... args) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // process -D property definitions
        args = new SystemPropertyArgParser().processArgs(args);

        // process class path
        PathParser path = new PathParser();
        args = path.processArgs(args);

        ClassLoader loader = getClassLoader(
                Thread.currentThread().getContextClassLoader(),
                path.getElements());

        Launcher launcher = new Launcher(loader, ODDJOB_MAIN_CLASS, args);

        launcher.launch();
    }

    public static void stop(String... args) throws Exception {
        Objects.requireNonNull(stop, "Launcher not running.")
                .close();
    }
}
