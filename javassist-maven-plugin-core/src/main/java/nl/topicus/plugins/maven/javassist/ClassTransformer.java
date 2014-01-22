package nl.topicus.plugins.maven.javassist;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import org.sonatype.plexus.build.incremental.BuildContext;

public abstract class ClassTransformer {
	private BuildContext buildContext;

	private String defaultOutputDirectory;

	private String filterPackageName;

	private ILogger logger;

	public BuildContext getBuildContext() {
		return buildContext;
	}

	public void setBuildContext(BuildContext buildContext) {
		this.buildContext = buildContext;
	}

	public String getDefaultOutputDirectory() {
		return defaultOutputDirectory;
	}

	public void setDefaultOutputDirectory(String defaultOutputDirectory) {
		this.defaultOutputDirectory = defaultOutputDirectory;
	}

	public ILogger getLogger() {
		return logger;
	}

	public void setLogger(ILogger logger) {
		this.logger = logger;
	}

	public String getFilterPackageName() {
		return filterPackageName;
	}

	protected abstract void applyTransformations(ClassPool classPool,
			CtClass classToTransform) throws Exception;

	protected boolean filterCtClass(final CtClass candidateClass)
			throws Exception {
		return true;
	}

	protected boolean filterClassName(String className) {
		if (filterPackageName != null && filterPackageName.length() > 0)
			return className.startsWith(filterPackageName);

		return false;
	}

	private ClassFileIterator createClassNameIterator(final String classPath) {
		if (new File(classPath).isDirectory()) {
			return new ClassNameDirectoryIterator(classPath, buildContext);
		} else {
			return new ClassNameJarIterator(classPath, buildContext);
		}
	}

	protected void configure(final Properties properties) throws Exception {
		if (properties == null)
			return;

		this.filterPackageName = properties.getProperty("filterPackageName");
	}

	protected void writeFile(CtClass candidateClass, String targetDirectory)
			throws Exception {
		String classname = candidateClass.getName();
		String filename = targetDirectory + File.separatorChar
				+ classname.replace('.', File.separatorChar) + ".class";
		int pos = filename.lastIndexOf(File.separatorChar);
		if (pos > 0) {
			String dir = filename.substring(0, pos);
			if (!dir.equals(".")) {
				File outputDir = new File(dir);
				outputDir.mkdirs();
				buildContext.refresh(outputDir);
			}
		}
		try (DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(
						buildContext.newFileOutputStream(new File(filename))))) {
			candidateClass.toBytecode(out);
		}
	}

	public final void transform(final List<String> classPaths) {
		int errors = 0;
		if (classPaths.isEmpty())
			return;

		final ClassPool classPool = new ClassPool(ClassPool.getDefault());
		classPool.appendClassPath(new LoaderClassPath(Thread.currentThread()
				.getContextClassLoader()));

		final Iterator<String> classPathIterator = classPaths.iterator();
		while (classPathIterator.hasNext()) {
			final String classPath = classPathIterator.next();
			getLogger().debug("Processing " + classPath);
			final ClassFileIterator classNames = createClassNameIterator(classPath);
			while (classNames.hasNext()) {
				final String className = classNames.next();
				if (!filterClassName(className))
					continue;

				try {
					classPool.importPackage(className);
					final CtClass candidateClass = classPool.get(className);
					if (filterCtClass(candidateClass)) {
						applyTransformations(classPool, candidateClass);
						writeFile(candidateClass, classPath);
					}
				} catch (final TransformationException e) {
					errors++;
					getBuildContext().addMessage(classNames.getLastFile(), 1,
							1, e.getMessage(), BuildContext.SEVERITY_ERROR,
							null);
					continue;
				} catch (final NotFoundException e) {
					errors++;
					getBuildContext().addMessage(
							classNames.getLastFile(),
							1,
							1,
							String.format("Class %s could not be resolved due "
									+ "to dependencies not found on current "
									+ "classpath.", className),
							BuildContext.SEVERITY_ERROR, e);
					continue;
				} catch (final Exception e) {
					errors++;
					getBuildContext().addMessage(
							classNames.getLastFile(),
							1,
							1,
							String.format("Class %s could not be transformed.",
									className), BuildContext.SEVERITY_ERROR, e);
					continue;
				}
			}
		}
		if (errors > 0)
			throw new TransformationException(errors
					+ " errors found during transformation.");
	}
}
