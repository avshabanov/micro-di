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

