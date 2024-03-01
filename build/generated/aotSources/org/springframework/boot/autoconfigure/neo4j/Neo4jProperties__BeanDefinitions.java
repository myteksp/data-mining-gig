package org.springframework.boot.autoconfigure.neo4j;

import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Bean definitions for {@link Neo4jProperties}.
 */
@Generated
public class Neo4jProperties__BeanDefinitions {
  /**
   * Get the bean definition for 'neo4jProperties'.
   */
  public static BeanDefinition getNeojPropertiesBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Neo4jProperties.class);
    beanDefinition.setInstanceSupplier(Neo4jProperties::new);
    return beanDefinition;
  }
}
