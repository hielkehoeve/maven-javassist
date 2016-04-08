javassist-maven-plugin
======================

A javassist maven plugin designed to alter class files after compilation. By creating your own ClassTransformer you can remove/add/edit code from/to/in your classes.

HelloWorldTransformer extends ClassTransformer
======================

```
protected void applyTransformations(CtClass classToTransform) throws Exception
```
is where you do the actual work. Some operations are quite tricky in javassist.

Add the following to your pom.xml and this plugin will run the HelloWorldTransformer on all classes in (sub)package nl.topicus.hello but not nl.topicus.hello.country or nl.topicus.hello.universe. All 3 configuration lists are accessable in your ClassTransformer.
======================

<?xml version="1.0" encoding="UTF-8"?>
<project ...>
	...
	
	<dependencies>
		...
		<dependency>
			<groupId>nl.topicus.plugins</groupId>
			<artifactId>javassist-maven-plugin-core</artifactId>
			<version>1.0-SNAPSHOT</version>
			<scope>provided</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			...
			<plugin>
				<groupId>nl.topicus.plugins</groupId>
				<artifactId>javassist-maven-plugin</artifactId>
				<version>1.0-SNAPSHOT</version>
				<configuration>
					<transformerClass>com.example.HelloWorldTransformer</transformerClass>
					<processInclusions>
						<inclusion>nl.topicus.hello</<inclusion>
					</processInclusions>
					<processExclusions>
						<exclusion>nl.topicus.hello.country</<exclusion>
						<exclusion>nl.topicus.hello.universe</<exclusion>
					</processExclusions>
					<exclusions>
						<exclusion>nl.topicus.bye</<exclusion>
					</exclusions>
				</configuration>
				<executions>
					<execution>
						<phase>process-classes</phase>
						<goals>
							<goal>javassist</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
</project>

