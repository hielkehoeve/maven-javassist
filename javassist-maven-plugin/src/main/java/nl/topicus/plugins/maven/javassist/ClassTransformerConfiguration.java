package nl.topicus.plugins.maven.javassist;

import java.util.Properties;

import org.apache.maven.plugins.annotations.Parameter;

public class ClassTransformerConfiguration {
	@Parameter(property = "className", required = true)
	private String className;
	
	@Parameter(property = "properties", required = false)
	private Properties properties;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}
}
