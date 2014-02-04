javassist-maven-plugin
======================

A javassist maven plugin designed to alter class files after compilation. By creating your own ClassTransformer you can remove/add/edit code from/to/in your classes.

Extend ClassTransformer
======================

```
protected boolean filterCtClass(CtClass candidateClass)
```
override this if you wish to filter some specific classes.

```
protected void applyTransformations(CtClass classToTransform) throws Exception
```
is where you do the actual work. Some operations are quite tricky in javassist.

Add the following to your pom.xml
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
					<packageName>com.example.entities</packageName>
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

