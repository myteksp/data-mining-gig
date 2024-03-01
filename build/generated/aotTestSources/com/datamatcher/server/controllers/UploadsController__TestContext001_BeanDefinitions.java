package com.datamatcher.server.controllers;

import com.datamatcher.server.services.UploadService;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link UploadsController}.
 */
@Generated
public class UploadsController__TestContext001_BeanDefinitions {
  /**
   * Get the bean instance supplier for 'uploadsController'.
   */
  private static BeanInstanceSupplier<UploadsController> getUploadsControllerInstanceSupplier() {
    return BeanInstanceSupplier.<UploadsController>forConstructor(UploadService.class)
            .withGenerator((registeredBean, args) -> new UploadsController(args.get(0)));
  }

  /**
   * Get the bean definition for 'uploadsController'.
   */
  public static BeanDefinition getUploadsControllerBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(UploadsController.class);
    beanDefinition.setInstanceSupplier(getUploadsControllerInstanceSupplier());
    return beanDefinition;
  }
}
