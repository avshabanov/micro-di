package com.truward.di;

import com.truward.di.support.DefaultInjectionContext;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link DefaultInjectionContext}.
 */
public class DefaultInjectionContextTest {

  private InjectionContext context;

  @Before
  public void initContext() {
    context = new DefaultInjectionContext();
  }

  public interface Inferior {
    int foo();
  }

  public interface Superior {
    int bar();
  }

  public static class InferiorImpl implements Inferior {
    @Override
    public int foo() {
      return 1;
    }
  }

  public static class SuperiorImpl implements Superior {
    @Resource
    protected Inferior inferior;

    @Override
    public int bar() {
      return 10 + inferior.foo();
    }
  }

  public static class InitializingSuperiorImpl implements Superior {
    @Resource
    private Inferior inferior;

    boolean postConstructInvoked = false;

    @Override
    public int bar() {
      return 10 + inferior.foo();
    }

    @PostConstruct
    public void postConstruct() {
      assertNotNull("All the resources references should be initialized", inferior);
      postConstructInvoked = true;
    }
  }

  public interface Inferior2 {
    int baz();
  }

  public static class Inferior2Impl implements Inferior2 {
    @Override
    public int baz() {
      return 1000;
    }
  }

  public static class Superior2Impl extends SuperiorImpl {
    @Resource
    private Inferior2 inferior2;

    @Override
    public int bar() {
      return 10000 + super.bar() + inferior2.baz();
    }
  }

  // public and static is a must, otherwise InjectionContext will not be able to
  // create the instance of this class via reflection
  public final static class SuperiorWithCtor {
    final int savedFoo;

    public SuperiorWithCtor(Inferior inferior) {
      savedFoo = inferior.foo();
    }
  }

  @Test
  public void shouldInjectOneBeanWithinAnother() {
    context.registerBean(new SuperiorImpl());
    context.registerBean(new InferiorImpl());

    final Superior superior = context.getBean(Superior.class);
    assertEquals(11, superior.bar());
  }

  @Test
  public void shouldRegisterBeanAndGetItByItsClass() {
    context.registerBean(new SuperiorImpl());
    context.registerBean(new InferiorImpl());

    final SuperiorImpl superior = context.getBean(SuperiorImpl.class);
    assertEquals(11, superior.bar());
  }

  @Test
  public void shouldInvokePostConstruct() {
    context.registerBean(new InitializingSuperiorImpl());
    context.registerBean(new InferiorImpl());

    final Superior superior = context.getBean(Superior.class);
    assertEquals(11, superior.bar());

    assertTrue("Ensure post construct called", ((InitializingSuperiorImpl) superior).postConstructInvoked);
  }

  @Test
  public void shouldPerformConstructorInjection() {
    context.registerBean(new InferiorImpl());
    context.registerBean(SuperiorWithCtor.class);

    final SuperiorWithCtor superior = context.getBean(SuperiorWithCtor.class);
    assertEquals(1, superior.savedFoo);
  }

  @Test
  public void shouldBeAbleToAccessBeanByParentInterface() {
    context.registerBean(new InferiorImpl());
    context.registerBean(new Inferior2Impl());
    context.registerBean(new Superior2Impl());

    final Superior superior = context.getBean(Superior.class);
    assertEquals(11011, superior.bar());
  }

  public interface Bar {
    int getBarCode();
  }

  public interface Baz {
    int getBazCode();
  }

  public interface BarBaz extends Bar, Baz {}

  public static class BarImpl implements Bar {
    @Override
    public int getBarCode() {
      return 1;
    }
  }

  public static class BazImpl implements Baz {
    @Override
    public int getBazCode() {
      return 2;
    }
  }

  public static class BarBazImpl implements BarBaz {
    @Override
    public int getBarCode() {
      return 3;
    }

    @Override
    public int getBazCode() {
      return 4;
    }
  }

  @Test
  public void shouldBeAbleToFetchBeanList() {
    context.registerBean(new BarImpl());
    context.registerBean(new BazImpl());
    context.registerBean(new BarBazImpl());
    context.freeze();

    assertEquals(2, context.getBeans(Bar.class).size());
    assertEquals(2, context.getBeans(Baz.class).size());
    assertEquals(1, context.getBeans(BarBaz.class).size());
    assertTrue(context.getBeans(Inferior.class).isEmpty());
  }

  public static final class ContextAware {
    @Resource
    InjectionContext context;
  }

  @Test
  public void shouldAutowireHostingContextWithinBean() {
    context.registerBean(ContextAware.class);

    final ContextAware contextAware = context.getBean(ContextAware.class);
    assertEquals(context, contextAware.context);
  }

  @Test(expected = InjectionException.class)
  public void shouldThrowInjectionExceptionOnUnknownBean() {
    context.getBean(Bar.class);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowIllegalStateOnModificationAfterFreeze() {
    context.freeze();
    context.registerBean(BarImpl.class);
  }
}
