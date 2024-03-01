package org.springframework.boot.autoconfigure.neo4j;

import org.neo4j.driver.Driver;
import org.springframework.aot.generate.Generated;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.aot.BeanInstanceSupplier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.env.Environment;

/**
 * Bean definitions for {@link Neo4jAutoConfiguration}.
 */
@Generated
public class Neo4jAutoConfiguration__TestContext001_BeanDefinitions {
  /**
   * Get the bean definition for 'neo4jAutoConfiguration'.
   */
  public static BeanDefinition getNeojAutoConfigurationBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Neo4jAutoConfiguration.class);
    beanDefinition.setInstanceSupplier(Neo4jAutoConfiguration::new);
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'neo4jConnectionDetails'.
   */
  private static BeanInstanceSupplier<Neo4jAutoConfiguration.PropertiesNeo4jConnectionDetails> getNeojConnectionDetailsInstanceSupplier(
      ) {
    return BeanInstanceSupplier.<Neo4jAutoConfiguration.PropertiesNeo4jConnectionDetails>forFactoryMethod(Neo4jAutoConfiguration.class, "neo4jConnectionDetails", Neo4jProperties.class, ObjectProvider.class)
            .withGenerator((registeredBean, args) -> registeredBean.getBeanFactory().getBean(Neo4jAutoConfiguration.class).neo4jConnectionDetails(args.get(0), args.get(1)));
  }

  /**
   * Get the bean definition for 'neo4jConnectionDetails'.
   */
  public static BeanDefinition getNeojConnectionDetailsBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Neo4jAutoConfiguration.PropertiesNeo4jConnectionDetails.class);
    beanDefinition.setInstanceSupplier(getNeojConnectionDetailsInstanceSupplier());
    return beanDefinition;
  }

  /**
   * Get the bean instance supplier for 'neo4jDriver'.
   */
  private static BeanInstanceSupplier<Driver> getNeojDriverInstanceSupplier() {
    return BeanInstanceSupplier.<Driver>forFactoryMethod(Neo4jAutoConfiguration.class, "neo4jDriver", Neo4jProperties.class, Environment.class, Neo4jConnectionDetails.class, ObjectProvider.class)
            .withGenerator((registeredBean, args) -> registeredBean.getBeanFactory().getBean(Neo4jAutoConfiguration.class).neo4jDriver(args.get(0), args.get(1), args.get(2), args.get(3)));
  }

  /**
   * Get the bean definition for 'neo4jDriver'.
   */
  public static BeanDefinition getNeojDriverBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Driver.class);
    beanDefinition.setDestroyMethodNames("close");
    beanDefinition.setInstanceSupplier(getNeojDriverInstanceSupplier());
    return beanDefinition;
  }
}
