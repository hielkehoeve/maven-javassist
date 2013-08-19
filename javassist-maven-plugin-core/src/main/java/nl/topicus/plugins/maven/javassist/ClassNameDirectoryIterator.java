package nl.topicus.plugins.maven.javassist;

import static org.apache.commons.io.FilenameUtils.removeExtension;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;

public class ClassNameDirectoryIterator implements Iterator<String> {
	final String classPath;
	Iterator<File> classFiles = new ArrayList<File>().iterator();

	public ClassNameDirectoryIterator( final String classPath) {
		this.classPath = classPath;
		this.classFiles = FileUtils.iterateFiles(new File(classPath), new String[]{ "class" }, true);
	}

	@Override
	public boolean hasNext() {
		return classFiles.hasNext();
	}

	@Override
	public String next() {
		final File classFile = classFiles.next();
		try {
			final String qualifiedFileName = classFile.getCanonicalPath().substring(classPath.length() + 1);
			return removeExtension(qualifiedFileName.replace(File.separator, "."));
		} catch (final IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public void remove() {
		classFiles.remove();
	}
}
