package org.springdoc.core.properties;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link SwaggerUiConfigProperties}.
 */
@Generated
public class SwaggerUiConfigProperties__BeanDefinitions {
  /**
   * Get the bean definition for 'swaggerUiConfigProperties'.
   */
  public static BeanDefinition getSwaggerUiConfigPropertiesBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(SwaggerUiConfigProperties.class);
    beanDefinition.setLazyInit(false);
    beanDefinition.setInstanceSupplier(SwaggerUiConfigProperties::new);
    return beanDefinition;
  }
}
