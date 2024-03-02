package com.datamatcher.server.controllers;

import com.datamatcher.server.services.DropboxService;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link DropboxController}.
 */
@Generated
public class DropboxController__BeanDefinitions {
  /**
   * Get the bean instance supplier for 'dropboxController'.
   */
  private static BeanInstanceSupplier<DropboxController> getDropboxControllerInstanceSupplier() {
    return BeanInstanceSupplier.<DropboxController>forConstructor(DropboxService.class)
            .withGenerator((registeredBean, args) -> new DropboxController(args.get(0)));
  }

  /**
   * Get the bean definition for 'dropboxController'.
   */
  public static BeanDefinition getDropboxControllerBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(DropboxController.class);
    beanDefinition.setInstanceSupplier(getDropboxControllerInstanceSupplier());
    return beanDefinition;
  }
}
