package org.onetwo.boot.core;

import javax.validation.Validator;

import org.onetwo.boot.core.config.BootBussinessConfig;
import org.onetwo.boot.core.config.BootJFishConfig;
import org.onetwo.boot.core.config.BootSpringConfig;
import org.onetwo.common.spring.validator.ValidatorWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.ClassUtils;

@Configuration
//@EnableConfigurationProperties({JFishBootConfig.class, SpringBootConfig.class})
@EnableConfigurationProperties({BootJFishConfig.class, BootSpringConfig.class, BootBussinessConfig.class})
public class BootContextConfig {
	
	public static final String BEAN_EXCEPTION_MESSAGE = "exceptionMessage";
	
	@Autowired
	private BootJFishConfig bootJFishConfig;

	@Bean
	public ValidatorWrapper validatorWrapper(){
		return ValidatorWrapper.wrap(validator());
	}
	
	@Bean
	public Validator validator() {
		Validator validator = null;
//		if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
		Class<?> clazz;
		try {
			String className = "org.springframework.validation.beanvalidation.LocalValidatorFactoryBean";
			clazz = ClassUtils.forName(className, org.onetwo.common.utils.ClassUtils.getDefaultClassLoader());
		} catch (ClassNotFoundException e) {
			throw new BeanInitializationException("Could not find default validator", e);
		} catch (LinkageError e) {
			throw new BeanInitializationException("Could not find default validator", e);
		}
		validator = (Validator) BeanUtils.instantiate(clazz);
//		LocalValidatorFactoryBean vfb = (LocalValidatorFactoryBean) validator;
//		vfb.setValidationMessageSource(messageSource());
//			vfb.setTraversableResolver(new EmptyTraversableResolver());
		return validator;
	}
	
	@Bean(name=BEAN_EXCEPTION_MESSAGE)
	public MessageSource exceptionMessageSource(){
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setCacheSeconds(bootJFishConfig.getMessageSource().getCacheSeconds());
		ms.setBasenames("classpath:messages/exception-messages", "classpath:messages/default-exception-messages");
		return ms;
	}
}
