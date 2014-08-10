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

package com.truward.di;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Provides very simple dependency injection support by
 * relying on the Resource annotation.
 *
 * All the beans assumed to be singletons in the current context.
 *
 * The role of the context is to initialize the given object's fields annotated with the
 * Resource annotation,
 * The methods annotated with the PostConstruct will be invoked when all these fields
 * will be initialized.
 *
 * The implementations of this class are not thread safe unless otherwise specified.
 *
 * @see javax.annotation.Resource
 * @see javax.annotation.PostConstruct
 *
 * @author Alexander Shabanov
 */
public interface InjectionContext {

  /**
   * Puts bean instance to the context.
   * The bean should be only one.
   *
   * @param bean Bean instance.
   * @param <T> Bean's class.
   */
  <T> void registerBean(@Nonnull T bean);

  /**
   * Puts bean instance associated with the given class.
   * Throws {@link InjectionException} if the given bean class clashes with certain existing definition.
   *
   * @param beanClass Non-interface bean class.
   * @param <T> Bean class type.
   */
  <T> void registerBean(@Nonnull Class<T> beanClass);

  /**
   * Gets bean associated with the class given.
   * Returns current Context instance when beanClass equals to Context class itself.
   * Throws {@link InjectionException} in case of no bean associated with the given class.
   * Throws {@link InjectionException} if multiple beans can be returned by this method.
   *
   * @param beanClass Interface class, that is expected to be associated with the bean.
   * @param <T> Interface type.
   * @return Non-null bean instance.
   */
  @Nonnull
  <T> T getBean(@Nonnull Class<T> beanClass);

  /**
   * Returns all the beans that implement the given class.
   *
   * @param beanClass Interface class, that is expected to be associated with the bean.
   * @param <T> Interface type.
   * @return Non-null list
   */
  @Nonnull
  <T> List<T> getBeans(@Nonnull Class<T> beanClass);

  /**
   * Freezes current context, so there will be no possibility to modify this context.
   * @see #isFrozen()
   */
  void freeze();

  /**
   * @return Boolean flag, that indicates, whether the context is frozen or not.
   * @see #freeze()
   */
  boolean isFrozen();
}
