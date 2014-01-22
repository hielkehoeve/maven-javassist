package nl.topicus.plugins.maven.javassist;

import static java.lang.Thread.currentThread;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Mojo(name = "javassist", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JavassistMojo extends AbstractMojo implements ILogger {

	private static final Class<ClassTransformer> TRANSFORMER_TYPE = ClassTransformer.class;

	@Component
	private BuildContext buildContext;

	@Parameter(defaultValue = "${project}", property = "project", required = true, readonly = true)
	private MavenProject project;

	@Parameter(property = "transformerClasses", required = true)
	private ClassTransformerConfiguration[] transformerClasses;

	public void execute() throws MojoExecutionException {
		final ClassLoader originalContextClassLoader = currentThread()
				.getContextClassLoader();
		try {
			final List<String> classpathElements = getRuntimeClasspathElements();
			loadClassPath(originalContextClassLoader,
					generateClassPathUrls(classpathElements));
			final List<ClassTransformer> transformers = instantiateTransformerClasses();

			for (ClassTransformer transformer : transformers) {
				transformer.transform(classpathElements);
			}
		} catch (TransformationException e) {
			getLog().error(e.getMessage());
			throw new MojoExecutionException(e.getMessage());
		} catch (final Exception e) {
			getLog().error(e.getMessage(), e);
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			currentThread().setContextClassLoader(originalContextClassLoader);
		}
	}

	private List<String> getRuntimeClasspathElements()
			throws DependencyResolutionRequiredException {
		List<?> ret = project.getCompileClasspathElements();
		ret.remove(project.getBuild().getOutputDirectory());
		return Lists.newArrayList(Iterables.filter(ret, String.class));
	}

	private List<URL> generateClassPathUrls(Iterable<String> classpathElements) {
		final List<URL> classPath = new ArrayList<URL>();
		for (final String runtimeResource : classpathElements) {
			URL url = resolveUrl(runtimeResource);
			if (url != null) {
				classPath.add(url);
			}
		}

		return classPath;
	}

	private void loadClassPath(final ClassLoader contextClassLoader,
			final List<URL> urls) {
		if (urls.size() <= 0)
			return;

		final URLClassLoader pluginClassLoader = URLClassLoader.newInstance(
				urls.toArray(new URL[urls.size()]), contextClassLoader);
		currentThread().setContextClassLoader(pluginClassLoader);
	}

	protected List<ClassTransformer> instantiateTransformerClasses()
			throws Exception {
		if (transformerClasses == null || transformerClasses.length <= 0)
			throw new MojoExecutionException(
					"Invalid transformer classes passed");

		final List<ClassTransformer> transformerInstances = new LinkedList<ClassTransformer>();
		for (ClassTransformerConfiguration transformerClass : transformerClasses) {
			transformerInstances
					.add(instantiateTransformerClass(transformerClass));
		}
		return transformerInstances;
	}

	protected ClassTransformer instantiateTransformerClass(
			final ClassTransformerConfiguration transformerClass)
			throws Exception {
		if (transformerClass == null
				|| transformerClass.getClassName().trim().isEmpty())
			throw new MojoExecutionException(
					"Invalid transformer class name passed");

		Class<?> transformerClassInstance = Class.forName(transformerClass
				.getClassName().trim(), true, currentThread()
				.getContextClassLoader());
		ClassTransformer transformerInstance = null;

		if (TRANSFORMER_TYPE.isAssignableFrom(transformerClassInstance)) {
			transformerInstance = TRANSFORMER_TYPE
					.cast(transformerClassInstance.newInstance());
			transformerInstance.setBuildContext(buildContext);
			transformerInstance.configure(transformerClass.getProperties());
			transformerInstance.setDefaultOutputDirectory(project.getBuild()
					.getOutputDirectory());
			transformerInstance.setLogger(this);
		} else {
			throw new MojoExecutionException(
					"Transformer class must inherit from "
							+ TRANSFORMER_TYPE.getName());
		}

		return transformerInstance;
	}

	private URL resolveUrl(final String resource) {
		try {
			return new File(resource).toURI().toURL();
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public void debug(String message) {
		getLog().debug(message);
	}

	@Override
	public void debug(String message, Throwable throwable) {
		getLog().debug(message, throwable);
	}

	@Override
	public void info(String message) {
		getLog().info(message);
	}

	@Override
	public void info(String message, Throwable throwable) {
		getLog().info(message, throwable);
	}

	@Override
	public void warn(String message) {
		getLog().warn(message);
	}

	@Override
	public void warn(String message, Throwable throwable) {
		getLog().warn(message, throwable);
	}

	@Override
	public void error(String message) {
		getLog().error(message);
	}

	@Override
	public void error(String message, Throwable throwable) {
		getLog().error(message, throwable);
	}
}
