package nl.topicus.plugins.maven.javassist;

import static java.lang.Thread.currentThread;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

@Mojo(name = "javassist", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JavassistMojo extends AbstractMojo implements ILogger {

	private static final Class<ClassTransformer> TRANSFORMER_TYPE = ClassTransformer.class;

	private static final Class<ClassWriter> WRITER_TYPE = ClassWriter.class;

	@Component
	private BuildContext buildContext;

	@Parameter(property = "project", defaultValue = "${project}")
	private MavenProject project;

	@Parameter(property = "transformerClass", required = true)
	private String transformerClass;

	@Parameter(property = "writerClass")
	private String writerClass;

	@Parameter(property = "processInclusions", required = true)
	private List<String> processInclusions;

	@Parameter(property = "processExclusions")
	private List<String> processExclusions;

	@Parameter(property = "exclusions")
	private List<String> exclusions;

	@Parameter(property = "outputDirectory", defaultValue = "${project.build.outputDirectory}")
	private String outputDirectory;

	public void execute() throws MojoExecutionException {
		final ClassLoader originalContextClassLoader = currentThread().getContextClassLoader();
		try {
			final List<String> classpathElements = project.getCompileClasspathElements();

			setContextClassLoader(originalContextClassLoader, classpathElements);
			process(classpathElements);

		} catch (DependencyResolutionRequiredException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			currentThread().setContextClassLoader(originalContextClassLoader);
		}
	}

	public final void process(final List<String> classpathElements) throws MojoExecutionException {
		int errors = 0;
		if (classpathElements.isEmpty())
			return;

		ClassTransformer transformer = instantiateTransformerClass();
		ClassWriter writer = instantiateWriterClass();

		final ClassPool classPool = new ClassPool(ClassPool.getDefault());
		classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));

		final Iterator<String> classPathIterator = classpathElements.iterator();
		while (classPathIterator.hasNext()) {
			final String classPath = classPathIterator.next();
			debug("Processing " + classPath);
			final ClassFileIterator classNames = createClassNameIterator(classPath);
			while (classNames.hasNext()) {
				final String className = classNames.next();
				try {
					final CtClass candidateClass = classPool.get(className);
					if (candidateClass.isFrozen() || !transformer.processClassName(className)) {
						debug("Skipping " + className);
						continue;
					}

					transformer.applyTransformations(classPool, candidateClass);
					writer.writeFile(buildContext, candidateClass, outputDirectory);
				} catch (final TransformationException e) {
					errors++;
					addMessage(classNames.getLastFile(), 1, 1, e.getMessage(), e);
					continue;
				} catch (final WriteException e) {
					errors++;
					addMessage(classNames.getLastFile(), 1, 1, e.getMessage(), e);
					continue;
				} catch (final NotFoundException e) {
					errors++;
					addMessage(classNames.getLastFile(), 1, 1, String.format(
							"Class %s could not be resolved due " + "to dependencies not found on current classpath.",
							className), e);
					continue;
				} catch (final Exception e) {
					errors++;
					addMessage(classNames.getLastFile(), 1, 1,
							String.format("Class %s could not be transformed.", className), e);
					continue;
				}
			}
		}
		if (errors > 0)
			throw new MojoExecutionException(errors + " errors found during transformation.");
	}

	protected void setContextClassLoader(ClassLoader originalContextClassLoader, List<String> classpathElements) {
		if (classpathElements.isEmpty())
			return;

		List<URL> classPathUrls = classpathElements.stream().map((e) -> {
			try {
				File file = new File(e);
				URI uri = file.toURI();
				return uri.toURL();
			} catch (Exception e2) {
				return null;
			}
		}).filter(e -> e != null).collect(Collectors.toList());

		currentThread().setContextClassLoader(URLClassLoader
				.newInstance(classPathUrls.toArray(new URL[classPathUrls.size()]), originalContextClassLoader));
	}

	protected ClassFileIterator createClassNameIterator(final String classPath) {
		if (new File(classPath).isDirectory()) {
			return new ClassNameDirectoryIterator(classPath, buildContext);
		} else {
			return new ClassNameJarIterator(classPath, buildContext);
		}
	}

	protected ClassTransformer instantiateTransformerClass() throws MojoExecutionException {
		if (transformerClass == null || transformerClass.trim().isEmpty())
			throw new MojoExecutionException("Invalid transformer class name passed");

		Class<?> transformerClassInstance;
		try {
			transformerClassInstance = Class.forName(transformerClass.trim(), true,
					currentThread().getContextClassLoader());
		} catch (ClassNotFoundException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		ClassTransformer transformerInstance = null;

		if (TRANSFORMER_TYPE.isAssignableFrom(transformerClassInstance)) {
			try {
				transformerInstance = TRANSFORMER_TYPE.cast(transformerClassInstance.newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				throw new MojoExecutionException(e.getMessage(), e);
			}
			transformerInstance.setLogger(this);
			transformerInstance.setProcessInclusions(processInclusions);
			transformerInstance.setProcessExclusions(processExclusions);
			transformerInstance.setExclusions(exclusions);
		} else {
			throw new MojoExecutionException("Transformer class must inherit from " + TRANSFORMER_TYPE.getName());
		}

		return transformerInstance;
	}

	protected ClassWriter instantiateWriterClass() throws MojoExecutionException {
		if (writerClass == null || writerClass.trim().isEmpty())
			return new ClassWriter();

		Class<?> writerClassInstance;
		try {
			writerClassInstance = Class.forName(writerClass.trim(), true, currentThread().getContextClassLoader());
			if (WRITER_TYPE.isAssignableFrom(writerClassInstance)) {
				ClassWriter writerInstance = WRITER_TYPE.cast(writerClassInstance.newInstance());
				writerInstance.setLogger(this);
				return writerInstance;
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			return new ClassWriter();
		}

		return new ClassWriter();
	}

	@Override
	public void addMessage(File file, int line, int pos, String message, Throwable e) {
		buildContext.addMessage(file, line, pos, message, BuildContext.SEVERITY_ERROR, e);
	}

	@Override
	public void debug(String message) {
		getLog().info(message);
	}

	@Override
	public void debug(String message, Throwable throwable) {
		getLog().info(message, throwable);
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
