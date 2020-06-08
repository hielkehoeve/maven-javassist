package nl.topicus.plugins.maven.javassist;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import org.sonatype.plexus.build.incremental.BuildContext;

import javassist.CannotCompileException;
import javassist.CtClass;

public class ClassWriter {

	private boolean defrost;
	
	private ILogger logger;

	public boolean isDefrost() {
		return defrost;
	}
	
	public void setDefrost(boolean defrost) {
		this.defrost = defrost;
	}
	
	public ILogger getLogger() {
		return logger;
	}

	public void setLogger(ILogger logger) {
		this.logger = logger;
	}

	public void writeFile(BuildContext buildContext, CtClass candidateClass, String targetDirectory)
			throws WriteException {
		
		if(isDefrost()) {
			candidateClass.defrost();
		}
		
		candidateClass.getClassFile().compact();
		candidateClass.rebuildClassFile();

		String classname = candidateClass.getName();
		String filename = targetDirectory + File.separatorChar + classname.replace('.', File.separatorChar) + ".class";
		File outputDir = new File(filename.substring(0, filename.lastIndexOf(File.separatorChar)));
		if (!outputDir.getPath().equals(".")) {
			outputDir.mkdirs();
			buildContext.refresh(outputDir);
		}
		try (DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(buildContext.newFileOutputStream(new File(filename))))) {
			candidateClass.toBytecode(out);
			buildContext.refresh(outputDir);
		} catch (IOException | CannotCompileException e) {
			throw new WriteException(e);
		}
	}
}
