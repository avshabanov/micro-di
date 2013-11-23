micro-di
========

Micro Dependency Injection Framework for Java (~10Kb)

## Sample Usage

```java
public interface Foo {
  int foo();
}

public interface Bar {
  int bar();
}

public class FooImpl implements Foo {
  @Override public int foo() { return 1; }
}

public class BarImpl implements Bar {
  @Resource private Foo foo;

  @Override public int bar() { return 10 + foo.foo(); }
}

// usage:
InjectionContext context = new DefaultInjectionContext();
context.registerBean(new FooImpl());
context.registerBean(new BarImpl());
context.freeze(); // "locks" context, so that it becames read-only

final Foo foo = context.getBean(Foo.class);
// actions on foo
```

# Compiling from sources and installing to the local maven repository

Just do ``mvn clean install`` in source folder

# Adding to maven project with minimal fuss

In your pom.xml or in your settings.xml add the following repository:

```xml
<repositories>
  <repository>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
    <id>custom-central</id>
    <name>libs-release</name>
    <url>https://raw.github.com/avshabanov/maven-repo/master/libs-release</url>
  </repository>
</repositories>
```

and then add jar dependency in your pom.xml:

```xml
<dependency>
  <groupId>com.truward.di</groupId>
  <artifactId>micro-di</artifactId>
  <version>1.0.3</version>
</dependency>
```

Have fun!

