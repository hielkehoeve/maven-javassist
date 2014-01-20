package nl.topicus.plugins.maven.javassist;

import static org.apache.commons.io.FilenameUtils.removeExtension;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class ClassNameJarIterator implements Iterator<String> {
	private Iterator<String> classFiles = new ArrayList<String>().iterator();

	public ClassNameJarIterator(final String classPath) {

		List<String> classNames = new ArrayList<>();
		try {
			JarInputStream jarFile = new JarInputStream(new FileInputStream(
					classPath));
			JarEntry jarEntry;

			while (true) {
				jarEntry = jarFile.getNextJarEntry();
				if (jarEntry == null)
					break;

				if (jarEntry.getName().endsWith(".class"))
					classNames.add(jarEntry.getName().replaceAll("/", "\\."));

			}

			jarFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		classFiles = classNames.iterator();
	}

	@Override
	public boolean hasNext() {
		return classFiles.hasNext();
	}

	@Override
	public String next() {
		return removeExtension(classFiles.next().replace(File.separator, "."));
	}

	@Override
	public void remove() {
		classFiles.remove();
	}
}
