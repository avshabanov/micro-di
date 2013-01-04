micro-di
========

Micro Dependency Injection Framework for Java (~10Kb)

## Sample Usage

    public interface Inferior {
        int foo();
    }

    public interface Superior {
        int bar();
    }

    public class InferiorImpl implements Inferior {
        @Override public int foo() { return 1; }
    }

    public class SuperiorImpl implements Superior {
        @Resource protected Inferior inferior;

        @Override public int bar() { return 10 + inferior.foo(); }
    }

    @Test
    public void shouldInjectOneBeanWithinAnother() {
        context.registerBean(new SuperiorImpl());
        context.registerBean(new InferiorImpl());

        final Superior superior = context.getBean(Superior.class);
        assertEquals(11, superior.bar());
    }

# Adding to maven project

In your pom.xml or in your settings.xml add the following repository:

    <repositories>
        <repository>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
            <id>central</id>
            <name>libs-release</name>
            <url>https://github.com/avshabanov/maven-repo/raw/master/libs-release</url>
        </repository>
    </repositories>

and then add jar dependency in your pom.xml:

    <dependency>
        <groupId>com.truward.di</groupId>
        <artifactId>micro-di</artifactId>
        <version>1.0.0</version>
    </dependency>

Have fun!

