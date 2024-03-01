package com.datamatcher.server.repositories;

import java.lang.String;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link RecordsRepo}.
 */
@Generated
public class RecordsRepo__TestContext001_BeanDefinitions {
  /**
   * Get the bean instance supplier for 'recordsRepo'.
   */
  private static BeanInstanceSupplier<RecordsRepo> getRecordsRepoInstanceSupplier() {
    return BeanInstanceSupplier.<RecordsRepo>forConstructor(String.class, String.class, String.class, String.class)
            .withGenerator((registeredBean, args) -> new RecordsRepo(args.get(0), args.get(1), args.get(2), args.get(3)));
  }

  /**
   * Get the bean definition for 'recordsRepo'.
   */
  public static BeanDefinition getRecordsRepoBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(RecordsRepo.class);
    beanDefinition.setDestroyMethodNames("com.datamatcher.server.repositories.RecordsRepo.close");
    beanDefinition.setInstanceSupplier(getRecordsRepoInstanceSupplier());
    return beanDefinition;
  }
}
