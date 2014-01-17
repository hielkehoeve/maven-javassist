package nl.topicus.plugins.maven.javassist;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

public abstract class ClassTransformer {

	private String defaultOutputDirectory;
	
	private String filterPackageName;

	private ILogger logger;

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

	protected abstract void applyTransformations(ClassPool classPool, CtClass classToTransform)
			throws Exception;

	protected boolean filterCtClass(final CtClass candidateClass) throws Exception {
		return true;
	}
	
	protected boolean filterClassName(String className) {
		if(filterPackageName != null && filterPackageName.length()>0)
			return className.startsWith(filterPackageName);
		
		return false;
	}

	private Iterator<String> createClassNameIterator(final String classPath)
			throws Exception {
		if (new File(classPath).isDirectory()) {
			return new ClassNameDirectoryIterator(classPath);
		} else {
			return new ClassNameJarIterator(classPath);
		}
	}

	protected void configure(final Properties properties) throws Exception {
		if(properties == null)
			return;
		
		this.filterPackageName = properties.getProperty("filterPackageName");
	}

	protected void writeFile(CtClass candidateClass, String classPath)
			throws Exception {
		candidateClass.writeFile(classPath);
	}

	public final void transform(final List<String> classPaths) {
		if (classPaths.isEmpty())
			return;

		try {
			// create new classpool for transform; don't blow up the default
			final ClassPool classPool = new ClassPool(ClassPool.getDefault());
			classPool.appendClassPath(new LoaderClassPath(Thread
					.currentThread().getContextClassLoader()));

			final Iterator<String> classPathIterator = classPaths.iterator();
			while (classPathIterator.hasNext()) {
				final String classPath = classPathIterator.next();
				final Iterator<String> classNames = createClassNameIterator(classPath);
				while (classNames.hasNext()) {
					final String className = classNames.next();
					if(!filterClassName(className))
						continue;
					
					try {
						classPool.importPackage(className);
						final CtClass candidateClass = classPool.get(className);
						if (filterCtClass(candidateClass)) {
							applyTransformations(classPool, candidateClass);
							writeFile(candidateClass, classPath);
						}
					} catch (final NotFoundException e) {
						getLogger()
								.warn(String
										.format("Class %s could not not be resolved due to dependencies not found on "
												+ "current classpath (usually your class depends on \"provided\" scoped dependencies).",
												className), e);
						continue;
					} catch (final Exception ex) {
						getLogger()
								.error(String.format(
										"Class %s could not not be instrumented due to initialize FAILED.",
										className), ex);
						continue;
					}
				}
			}
		} catch (final Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
