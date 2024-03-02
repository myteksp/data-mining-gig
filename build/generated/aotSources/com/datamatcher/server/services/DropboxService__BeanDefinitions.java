package com.datamatcher.server.services;

import com.datamatcher.server.repositories.DropBoxRepo;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link DropboxService}.
 */
@Generated
public class DropboxService__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'dropboxService'.
   */
  private static BeanInstanceSupplier<DropboxService> getDropboxServiceInstanceSupplier() {
    return BeanInstanceSupplier.<DropboxService>forConstructor(DropBoxRepo.class)
            .withGenerator((registeredBean, args) -> new DropboxService(args.get(0)));
  }

  /**
   * Get the bean definition for 'dropboxService'.
   */
  public static BeanDefinition getDropboxServiceBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(DropboxService.class);
    beanDefinition.setInstanceSupplier(getDropboxServiceInstanceSupplier());
    return beanDefinition;
  }
}
