package com.datamatcher.server.repositories;

import java.lang.String;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link DropBoxRepo}.
 */
@Generated
public class DropBoxRepo__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'dropBoxRepo'.
   */
  private static BeanInstanceSupplier<DropBoxRepo> getDropBoxRepoInstanceSupplier() {
    return BeanInstanceSupplier.<DropBoxRepo>forConstructor(String.class)
            .withGenerator((registeredBean, args) -> new DropBoxRepo(args.get(0)));
  }

  /**
   * Get the bean definition for 'dropBoxRepo'.
   */
  public static BeanDefinition getDropBoxRepoBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(DropBoxRepo.class);
    beanDefinition.setInstanceSupplier(getDropBoxRepoInstanceSupplier());
    return beanDefinition;
  }
}
