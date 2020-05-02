package com.jdbc.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;


@Mojo(name = "prepare-agent", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class AgentMojo extends AbstractMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("hello");
    }

//    private URL resolveUrl(final String resource) {
//        try {
//            return new File(resource).toURI().toURL();
//        } catch (final MalformedURLException e) {
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }



//    private ClassLoader getClassLoader(MavenProject project)
//    {
//        try
//        {
//            List<String> classpathElements = project.getCompileClasspathElements();
//            classpathElements.add( project.getBuild().getOutputDirectory() );
//            classpathElements.add( project.getBuild().getTestOutputDirectory() );
//            URL[] urls = new URL[classpathElements.size()];
//            for ( int i = 0; i < classpathElements.size(); ++i )
//            {
//                urls[i] = new File(classpathElements.get( i )).toURL();
//            }
//            return new URLClassLoader( urls, this.getClass().getClassLoader() );
//        }
//        catch ( Exception e )
//        {
//            getLog().debug( "Couldn't get the classloader." );
//            return this.getClass().getClassLoader();
//        }
//    }
//
//    /**
//     * This method sets up the environment that we need for testing. If no jar file is available in the current project
//     * the classes will first be compiled to be ready to be given to the Instrumentation agent of the plugin.
//     *
//     * @return
//     */
//    private Collection<Path> loadClasses() {
//        Path currentUserDirPath = Paths.get(System.getProperty("user.dir"));
//        Path targetDirPath = Paths.get(currentUserDirPath+"/target");
//
//        if(Files.exists(targetDirPath)){
//            Collection<Path> absClassPaths = new ArrayList<>();
//            Path classes = Paths.get(targetDirPath+"/classes");
//            try {
//                this.getClassPathsFromDirRecursive(classes, absClassPaths);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            getLog().info(Arrays.toString(absClassPaths.toArray()));
//            return  absClassPaths;
//        }
//        return null;
//    }
//
//    private void getClassPathsFromDirRecursive(Path directory, Collection<Path> pathCollection)
//            throws IOException {
//        try (DirectoryStream<Path> ds = Files.newDirectoryStream(directory)) {
//            for (Path child : ds) {
//                if (Files.isDirectory(child)) {
//                    getClassPathsFromDirRecursive(child, pathCollection);
//                } else {
//                    pathCollection.add(child);
//                }
//            }
//        }
//    }
}
