package nl.topicus.plugins.maven.javassist;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.StreamSupport;

import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.common.io.Files;

public class ClassNameDirectoryIterator implements ClassFileIterator {
	private final String classPath;
	private Iterator<File> classFiles = new ArrayList<File>().iterator();
	private File lastFile;

	public ClassNameDirectoryIterator(final String classPath, final BuildContext buildContext) {
		this.classPath = classPath;
		this.classFiles = StreamSupport
				.stream(Files.fileTraverser().depthFirstPreOrder(new File(classPath)).spliterator(), false)
				.filter(i -> "class".equals(Files.getFileExtension(i.getName())) && buildContext.hasDelta(i))
				.iterator();
	}

	@Override
	public boolean hasNext() {
		return classFiles.hasNext();
	}

	@Override
	public String next() {
		final File classFile = classFiles.next();
		lastFile = classFile;
		try {
			final String qualifiedFileName = classFile.getCanonicalPath().substring(classPath.length() + 1);
			return Files.getNameWithoutExtension(qualifiedFileName.replace(File.separator, "."));
		} catch (final IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public File getLastFile() {
		return lastFile;
	}

	@Override
	public void remove() {
		classFiles.remove();
	}
}
