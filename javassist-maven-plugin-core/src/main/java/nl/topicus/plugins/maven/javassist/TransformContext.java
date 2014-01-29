package nl.topicus.plugins.maven.javassist;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

public class TransformContext {
	private ClassPool classPool;
	private ClassTransformer transformer;

	public TransformContext(String transformerClass, String filterPackage) {
		classPool = new ClassPool(ClassPool.getDefault());
		try {
			transformer = (ClassTransformer) Class.forName(transformerClass,
					true, Thread.currentThread().getContextClassLoader())
					.newInstance();
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
		transformer.setFilterPackageName(filterPackage);
		transformer.setLogger(new ILogger() {

			@Override
			public void warn(String message, Throwable throwable) {
			}

			@Override
			public void warn(String message) {
			}

			@Override
			public void info(String message, Throwable throwable) {
			}

			@Override
			public void info(String message) {
			}

			@Override
			public void error(String message, Throwable throwable) {
			}

			@Override
			public void error(String message) {
			}

			@Override
			public void debug(String message, Throwable throwable) {
			}

			@Override
			public void debug(String message) {
			}

			@Override
			public void addMessage(File file, int line, int pos,
					String message, Throwable e) {
			}
		});
	}

	public void appendClassPath(String classPath) {
		try {
			classPool.appendClassPath(classPath);
		} catch (NotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	public byte[] transform(String className) {
		if (transformer.filterClassName(className)) {
			try {
				CtClass classToTransform = classPool.get(className);
				if (!classToTransform.isFrozen()
						&& transformer.filterCtClass(classToTransform)) {
					transformer.applyTransformations(classPool,
							classToTransform);
					classToTransform.getClassFile().compact();
					classToTransform.rebuildClassFile();

					try (ByteArrayOutputStream baos = new ByteArrayOutputStream(
							4096);
							DataOutputStream out = new DataOutputStream(
									new BufferedOutputStream(baos))) {
						classToTransform.toBytecode(out);
						out.flush();
						return baos.toByteArray();
					} catch (IOException | CannotCompileException e) {
						throw new IllegalStateException(e);
					}
				}
			} catch (NotFoundException e) {
				throw new IllegalStateException(e);
			}
		}
		return null;
	}
}
