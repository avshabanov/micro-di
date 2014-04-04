/*
 * Copyright 2012 Alexander Shabanov - http://alexshabanov.com.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.truward.di.support;

import com.truward.di.InjectionContext;
import com.truward.di.InjectionException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Default implementation of {@link InjectionContext}.
 *
 * @author Alexander Shabanov
 */
public class DefaultInjectionContext implements InjectionContext {
  private static final class BeanHolder<T> {
    boolean initialized = false;
    final T bean;

    BeanHolder(T bean) {
      this.bean = bean;
    }
  }

  /**
   * The underlying bean definitions.
   */
  private List<BeanHolder<?>> beanHolders = new ArrayList<BeanHolder<?>>();

  /**
   * Cached beans definitions.
   */
  private Map<Class<?>, BeanHolder<?>> cachedInterfaceMap = new HashMap<Class<?>, BeanHolder<?>>();

  /**
   * Cache miss marker.
   */
  private static final BeanHolder<Object> NIL_BEAN_HOLDER = new BeanHolder<Object>(Boolean.FALSE);

  private boolean frozen;

  @Override
  public <T> void registerBean(T bean) {
    shouldNotBeFrozen();
    addUninitializedBean(bean);
  }

  @Override
  public <T> void registerBean(Class<T> beanClass) {
    shouldNotBeFrozen();
    try {
      addUninitializedBean(constructBean(beanClass));
    } catch (InstantiationException e) {
      throw new InjectionException(e);
    } catch (IllegalAccessException e) {
      throw new InjectionException(e);
    } catch (InvocationTargetException e) {
      throw new InjectionException(e);
    }
  }

  @Override
  public <T> T getBean(Class<T> beanClass) {
    // special case: Context requested
    if (beanClass.equals(InjectionContext.class)) {
      return beanClass.cast(this);
    }

    return getInitializedBean(findBeanHolder(beanClass), beanClass);
  }

  @Override
  public <T> List<T> getBeans(Class<T> beanClass) {
    final BeanHolder<?> beanHolder = cachedInterfaceMap.get(beanClass);

    if (beanHolder == null) {
      return Collections.emptyList();
    }

    if (beanHolder != NIL_BEAN_HOLDER) {
      return Collections.singletonList(getInitializedBean(beanHolder, beanClass));
    }

    // get bean holder by direct access (uncached)
    final List<T> beans = new ArrayList<T>();
    for (final BeanHolder<?> holder : beanHolders) {
      if (beanClass.isAssignableFrom(holder.bean.getClass())) {
        beans.add(getInitializedBean(holder, beanClass));
      }
    }

    return Collections.unmodifiableList(beans);
  }

  @Override
  public void freeze() {
    frozen = true;
  }

  @Override
  public boolean isFrozen() {
    return frozen;
  }

  //
  // Private
  //

  private void shouldNotBeFrozen() {
    if (isFrozen()) {
      throw new IllegalStateException("Modifications are not allowed for frozen injection context");
    }
  }


  private static void addInterfacesToSink(Set<Class<?>> interfaceSink, Class<?> targetClass) {
    if (!targetClass.isInterface()) {
      final Class<?> superclass = targetClass.getSuperclass();
      if (superclass != null) {
        addInterfacesToSink(interfaceSink, superclass);
      }
    }

    for (final Class<?> interfaceClass : targetClass.getInterfaces()) {
      interfaceSink.add(interfaceClass);
      addInterfacesToSink(interfaceSink, interfaceClass);
    }
  }

  private void cacheInterfaces(BeanHolder<?> beanHolder) {
    final Set<Class<?>> interfaceSink = new HashSet<Class<?>>();
    addInterfacesToSink(interfaceSink, beanHolder.bean.getClass());

    // cache all the definitions
    for (final Class<?> interfaceClass : interfaceSink) {
      final BeanHolder<?> assoc = cachedInterfaceMap.get(interfaceClass);

      // mark as cache miss
      if (assoc != null) {
        if (assoc != NIL_BEAN_HOLDER) {
          cachedInterfaceMap.put(interfaceClass, NIL_BEAN_HOLDER);
        }
        continue;
      }

      cachedInterfaceMap.put(interfaceClass, beanHolder);
    }
  }

  private BeanHolder<?> findBeanHolder(Class<?> beanClass) {
    // try get bean holder from cache
    BeanHolder<?> beanHolder = cachedInterfaceMap.get(beanClass);
    if (beanHolder != null && beanHolder != NIL_BEAN_HOLDER) {
      return beanHolder;
    }

    beanHolder = null; // reset sentinel so that we'll get a correct message about duplicate bean definitions

    // get bean holder by direct access (uncached)
    for (final BeanHolder<?> holder : beanHolders) {
      if (beanClass.isAssignableFrom(holder.bean.getClass())) {
        if (beanHolder != null) {
          throw new InjectionException("Ambigous definition for class " + beanClass +
              " conflicting definitions are: " + holder.bean + " and " + beanHolder.bean);
        }

        beanHolder = holder;
      }
    }

    if (beanHolder == null) {
      throw new InjectionException("The requested bean of class " + beanClass + " has not been found");
    }

    return beanHolder;
  }


  private <T> void addUninitializedBean(T bean) {
    if (bean == null) {
      throw new NullPointerException("Bean instance is null");
    }

    // check, that this bean is unique and there is no already defined bean with exactly the same class,
    // as in this case clashes is inevitable.
    for (final BeanHolder<?> beanHolder : beanHolders) {
      if (beanHolder.bean == bean) {
        throw new InjectionException("Duplicate declaration of bean " + bean);
      }

      if (beanHolder.bean.getClass().equals(bean.getClass())) {
        throw new InjectionException("The context already have definition of bean with class " + bean.getClass());
      }
    }

    final BeanHolder<T> beanHolder = new BeanHolder<T>(bean);
    beanHolders.add(beanHolder);
    cacheInterfaces(beanHolder);
  }

  // creates bean for class-only putBean method
  private <T> T constructBean(Class<T> beanClass) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    if (beanClass.isInterface()) {
      throw new InjectionException("The given class is interface: " + beanClass);
    }

    T beanInstance = null;

    // try to instantiate bean using constructor
    for (final Constructor<?> ctor : beanClass.getConstructors()) {
      if (beanInstance != null) {
        throw new InjectionException("Bean " + beanClass + " defines multiple constructors");
      }

      final Class<?>[] parameterTypes = ctor.getParameterTypes();
      final Object[] parameters = new Object[parameterTypes.length];

      for (int i = 0; i < parameters.length; ++i) {
        parameters[i] = getBean(parameterTypes[i]);
      }

      beanInstance = beanClass.cast(ctor.newInstance(parameters));
    }

    // try to use default constructor
    if (beanInstance == null) {
      beanInstance = beanClass.newInstance();
    }

    return beanInstance;
  }

  private <T> T getInitializedBean(BeanHolder<?> beanHolder, Class<T> beanClass) {
    assert beanHolder != null;

    // initialize all the fields
    if (!beanHolder.initialized) {
      try {
        initializeBeanHolder(beanHolder);
      } catch (IllegalAccessException e) {
        throw new InjectionException("Illegal access error when initializing class " + beanClass, e);
      } catch (InvocationTargetException e) {
        throw new InjectionException("Invocation error when initializing class " + beanClass, e);
      }
    }

    return beanClass.cast(beanHolder.bean);
  }

  // initializes bean within the bean holder
  private <T> void initializeBeanHolder(BeanHolder<T> beanHolder) throws IllegalAccessException, InvocationTargetException {
    // collect all the classes that may contain fields for further initialization.
    final List<Class<?>> classes = new ArrayList<Class<?>>();
    final Class<?> beanImplClass = beanHolder.bean.getClass();
    for (Class<?> c = beanImplClass; c != null && !c.equals(Object.class); c = c.getSuperclass()) {
      classes.add(c);
    }

    // initialize each referenced bean
    for (final Class<?> c : classes) {
      for (final Field field : c.getDeclaredFields()) {
        final Resource resourceAnnotation = field.getAnnotation(Resource.class);

        if (resourceAnnotation == null) {
          continue;
        }

        if (!resourceAnnotation.mappedName().isEmpty()) {
          throw new UnsupportedOperationException("Beans with mappedName are not supported, " +
              "class: " + beanImplClass + ", field: " + field.getName() + ", " +
              "mappedName: " + resourceAnnotation.mappedName());
        }

        final boolean wasAccessible = field.isAccessible();
        if (!wasAccessible) {
          field.setAccessible(true);
        }

        // normally we'd expect that fields are not initialized
        assert field.get(beanHolder.bean) == null;

        field.set(beanHolder.bean, getBean(field.getType()));

        if (!wasAccessible) {
          field.setAccessible(false);
        }
      }
    }

    // invoke post-construct methods
    assert beanImplClass != null;
    for (final Method method : beanImplClass.getMethods()) {
      if (method.getAnnotation(PostConstruct.class) == null) {
        continue;
      }

      // validate and invoke post-construct method
      if (method.getParameterTypes().length > 0) {
        throw new UnsupportedOperationException("Method " + method + " is declared as post construct, but " +
            "it takes parameters which is not supported");
      }

      method.invoke(beanHolder.bean);
    }

    // mark this bean as initialized one.
    beanHolder.initialized = true;
  }
}
