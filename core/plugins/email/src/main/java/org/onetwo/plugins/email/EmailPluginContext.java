package org.onetwo.plugins.email;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;

import org.onetwo.common.exception.ServiceException;
import org.onetwo.common.log.MyLoggerFactory;
import org.onetwo.common.spring.SpringUtils;
import org.onetwo.common.spring.ftl.StringFtlTemplateLoader;
import org.onetwo.common.utils.Assert;
import org.onetwo.common.utils.ReflectUtils;
import org.onetwo.common.utils.propconf.AppConfig;
import org.slf4j.Logger;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.google.common.collect.Sets;

@Configuration
public class EmailPluginContext implements InitializingBean {

	
	private final Logger logger = MyLoggerFactory.getLogger(this.getClass());

	@Resource
	private AppConfig appConfig;
	
//	@Resource
//	private Properties mailConfig;
	private EmailConfig mailConfig = EmailPlugin.getInstance().getConfig();
	
	@Resource
	private freemarker.template.Configuration mailFreemarkerConfiguration;
	
	private StringFtlTemplateLoader stringFtlTemplateLoader = new StringFtlTemplateLoader();

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(appConfig, "appConfig must not be null!");
	}

	@Bean
	public JavaMailService JavaMailService() throws Exception{
		Class<?> implClass = mailConfig.getMailServiceClass();
		if(!JavaMailServiceImpl.class.isAssignableFrom(implClass))
			throw new ServiceException("java mail sender must a instance of " + JavaMailSenderImpl.class.getName());
		
		JavaMailServiceImpl jm = (JavaMailServiceImpl)ReflectUtils.newInstance(implClass);
		jm.setJavaMailSender(javaMailSender());
		jm.setConfiguration(mailFreemarkerConfiguration);
		jm.setStringFtlTemplateLoader(stringFtlTemplateLoader);
		return jm;
	}
	
	@Bean
	public JavaMailSender javaMailSender() throws IOException {
		Set<String> excludeKeys = Sets.newHashSet();
		excludeKeys.add(EmailConfig.MAIL_SERVICE_CLASS_KEY);
		excludeKeys.add(EmailConfig.JAVA_MAIL_PROPERTIES_KEY);

		Class<?> implClass = mailConfig.getMailSender();
		if(!JavaMailSenderImpl.class.isAssignableFrom(implClass))
			throw new ServiceException("java mail sender must a instance of " + JavaMailSenderImpl.class.getName());
		
		JavaMailSenderImpl sender = (JavaMailSenderImpl)ReflectUtils.newInstance(implClass);
		Properties javaMailProperties = new Properties();
		Enumeration<String> names = (Enumeration<String>) mailConfig.getSourceConfig().propertyNames();
		BeanWrapper bw = SpringUtils.newBeanWrapper(sender);
		while (names.hasMoreElements()) {
			String propertyName = names.nextElement();
			String value = mailConfig.getSourceConfig().getProperty(propertyName);
			if (propertyName.startsWith(EmailConfig.JAVA_MAIL_PROPERTIES_KEY)) {
				propertyName = propertyName.substring(EmailConfig.JAVA_MAIL_PROPERTIES_KEY.length());
//				LangUtils.println("mail config : ${0}, ${1}", propertyName, value);
				javaMailProperties.setProperty(propertyName, value);
			}else if(excludeKeys.contains(propertyName)){
				logger.info("ignore mail config: {}", propertyName);
			} else {
				if(bw.isWritableProperty(propertyName))
					bw.setPropertyValue(propertyName, value);
			}
		}
		sender.setJavaMailProperties(javaMailProperties);
		return sender;
	}

	@Bean
	public EmailConfig mailConfig() {
		/*String envLocation = "/email/mailconfig-" + appConfig.getAppEnvironment() + ".properties";
		return SpringUtils.createPropertiesBySptring("/email/mailconfig.properties", envLocation);*/
		return mailConfig;
	}
	
	@Bean
	public FreeMarkerConfigurationFactoryBean mailFreemarkerConfiguration(){
		FreeMarkerConfigurationFactoryBean fcfb = new FreeMarkerConfigurationFactoryBean();
		fcfb.setTemplateLoaderPath("classpath:/email/ftl/");
		fcfb.setPreTemplateLoaders(stringFtlTemplateLoader);
		return fcfb;
	}

}
