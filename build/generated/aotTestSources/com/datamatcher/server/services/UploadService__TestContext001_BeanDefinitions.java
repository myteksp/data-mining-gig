package com.datamatcher.server.services;

import com.datamatcher.server.repositories.RecordsRepo;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link UploadService}.
 */
@Generated
public class UploadService__TestContext001_BeanDefinitions {
  /**
   * Get the bean instance supplier for 'uploadService'.
   */
  private static BeanInstanceSupplier<UploadService> getUploadServiceInstanceSupplier() {
    return BeanInstanceSupplier.<UploadService>forConstructor(RecordsRepo.class)
            .withGenerator((registeredBean, args) -> new UploadService(args.get(0)));
  }

  /**
   * Get the bean definition for 'uploadService'.
   */
  public static BeanDefinition getUploadServiceBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(UploadService.class);
    beanDefinition.setInstanceSupplier(getUploadServiceInstanceSupplier());
    return beanDefinition;
  }
}
