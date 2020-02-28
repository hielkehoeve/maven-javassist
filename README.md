javassist-maven-plugin
======================

A javassist maven plugin designed to alter class files after compilation. By creating your own ClassTransformer you can remove/add/edit code from/to/in your classes.

HelloWorldTransformer extends ClassTransformer
======================

```
protected void applyTransformations(CtClass classToTransform) throws Exception
```
is where you do the actual work. Some operations are quite tricky in javassist.


To make this work create the following setup:
======================

- **Project A**, containing the classes you wish to process.

- **Project A-transformer**, containing only the transformer class (you don't want this to end up in either A or A-android)

`src/main/java/nl/topicus/hello/transformer/AndroidTransformer.java`

```<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>nl.topicus.hello</groupId>
	<artifactId>A-transformer</artifactId>
	<version>1.0-SNAPSHOT</version>
	<packaging>jar</packaging>

	<name>A Android Transformer</name>

	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>nl.topicus.plugins</groupId>
				<artifactId>javassist-maven-plugin-core</artifactId>
				<version>2.1</version>
			</dependency>
		</dependencies>
	</dependencyManagement>

	<dependencies>
		<dependency>
			<groupId>nl.topicus.plugins</groupId>
			<artifactId>javassist-maven-plugin-core</artifactId>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-source-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
</project>
```


- **Project A-android**, containing nothing except a pom.xml

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>nl.topicus.hello</groupId>
	<artifactId>A-android</artifactId>
	<version>1.0-SNAPSHOT</version>
	<packaging>jar</packaging>

	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>nl.topicus.hello</groupId>
				<artifactId>A</artifactId>
				<version>${project.version}</version>
				<!-- set this to provided to make sure it won't be included in any android project -->
				<scope>provided</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>

	<dependencies>
		<dependency>
			<groupId>nl.topicus.hello</groupId>
			<artifactId>A</artifactId>
		</dependency>
	</dependencies>

	<build>
		<pluginManagement>
			<plugins>
				<plugin>
					<groupId>nl.topicus.plugins</groupId>
					<artifactId>javassist-maven-plugin</artifactId>
					<version>2.1</version>
					<dependencies>
						<dependency>
							<groupId>nl.topicus.hello</groupId>
							<artifactId>A-transformer</artifactId>
							<version>1.0-SNAPSHOT</version>
						</dependency>
					</dependencies>
					<configuration>
<transformerClass>nl.topicus.hello.AndroidTransformer</transformerClass>
						<processInclusions>
							<inclusion>nl.topicus.hello</inclusion>
						</processInclusions>
						<processExclusions>
							<exclusion>nl.topicus.cobra.hello.exception</exclusion>
						</processExclusions>
						<exclusions>
							<exclusion>javax</exclusion>
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
		</pluginManagement>

		<plugins>
			<plugin>
				<groupId>nl.topicus.plugins</groupId>
				<artifactId>javassist-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
</project>

```



and this plugin will run the HelloWorldTransformer on all classes in (sub)package nl.topicus.hello but not nl.topicus.hello.country or nl.topicus.hello.universe. All 3 configuration lists are accessable in your ClassTransformer.


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

