package nl.topicus.plugins.maven.javassist;

import static org.apache.commons.io.FilenameUtils.removeExtension;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

public class ClassNameDirectoryIterator implements ClassFileIterator {
	private final String classPath;
	private Iterator<File> classFiles = new ArrayList<File>().iterator();
	private File lastFile;

	public ClassNameDirectoryIterator(final String classPath,
			final BuildContext buildContext) {
		this.classPath = classPath;
		this.classFiles = FluentIterable
				.from(FileUtils.listFiles(new File(classPath),
						new String[] { "class" }, true))
				.filter(new Predicate<File>() {
					@Override
					public boolean apply(File input) {
						return buildContext.hasDelta(input);
					}
				}).iterator();
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
			final String qualifiedFileName = classFile.getCanonicalPath()
					.substring(classPath.length() + 1);
			return removeExtension(qualifiedFileName.replace(File.separator,
					"."));
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
