package org.onetwo.ext.ons.producer;

import java.util.List;
import java.util.Properties;

import org.onetwo.common.spring.SpringUtils;
import org.onetwo.ext.alimq.MessageSerializer;
import org.onetwo.ext.alimq.MessageSerializer.MessageDelegate;
import org.onetwo.ext.alimq.OnsMessage;
import org.onetwo.ext.alimq.SimpleMessage;
import org.onetwo.ext.ons.ONSProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.ons.api.bean.TransactionProducerBean;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionExecuter;
import com.aliyun.openservices.ons.api.transaction.TransactionStatus;

/**
 * ons普通producer和事务produer不能混用
 * @author wayshall
 * <br/>
 */
public class ONSTransactionProducerServiceImpl extends TransactionProducerBean implements InitializingBean, DisposableBean, TransactionProducerService {
	
	public static final LocalTransactionExecuter COMMIT_EXECUTER = (msg, arg)->{
		return TransactionStatus.CommitTransaction;
	};
	
//	private final Logger logger = JFishLoggerFactory.getLogger(this.getClass());
	
	private MessageSerializer messageSerializer;

	private ONSProperties onsProperties;
	private String producerId;
//	private ONSProducerListenerComposite producerListenerComposite;
	@Autowired
	private List<SendMessageInterceptor> sendMessageInterceptors;
	
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
//	private ConfigurableListableBeanFactory configurableListableBeanFactory;
	private FakeProducerService fakeProducerService = new FakeProducerService();
	
	
	@Autowired
	public void setOnsProperties(ONSProperties onsProperties) {
		this.onsProperties = onsProperties;
	}

	public void setProducerId(String producerId) {
		this.producerId = producerId;
	}

	@Autowired
	public void setMessageSerializer(MessageSerializer messageSerializer) {
		this.messageSerializer = messageSerializer;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(producerId);
		Assert.notNull(onsProperties);
		Assert.notNull(messageSerializer);

		AnnotationAwareOrderComparator.sort(sendMessageInterceptors);
		
		Properties producerProperties = onsProperties.baseProperties();
		Properties customProps = onsProperties.getProducers().get(producerId);
		if(customProps!=null){
			producerProperties.putAll(customProps);
		}
		producerProperties.setProperty(PropertyKeyConst.ProducerId, producerId);
		
		this.setProperties(producerProperties);
		this.start();
		
//		this.configurableListableBeanFactory.registerResolvableDependency(ProducerService.class, new ProducerServiceFactory());
	}
	
	@Override
	public void destroy() throws Exception {
		this.shutdown();
	}
	

	@Override
	public SendResult sendMessage(OnsMessage onsMessage, LocalTransactionExecuter executer, Object arg){
		Message message = onsMessage.toMessage();
		String topic = SpringUtils.resolvePlaceholders(applicationContext, message.getTopic());
		message.setTopic(topic);
		Object body = onsMessage.getBody();
		if(needSerialize(body)){
			message.setBody(this.messageSerializer.serialize(onsMessage.getBody(), new MessageDelegate(message)));
		}else{
			message.setBody((byte[])body);
		}
		return sendRawMessage(message, executer, arg);
	}

	protected boolean needSerialize(Object body){
		if(body==null){
			return false;
		}
		return !byte[].class.isInstance(body);
	}

	protected SendResult sendRawMessage(Message message, LocalTransactionExecuter executer, Object arg){
		SendMessageInterceptorChain chain = new SendMessageInterceptorChain(sendMessageInterceptors, ()->this.send(message, executer, arg));
		SendMessageContext ctx = SendMessageContext.builder()
													.message(message)
													.source(this)
													.transactionProducer(this)
													.chain(chain)
													.build();
		chain.setSendMessageContext(ctx);
		return chain.invoke();
	}
	
	@Override
	public boolean isTransactional() {
		return true;
	}

	@Override
	public <T> T getRawProducer(Class<T> targetClass) {
		return targetClass.cast(this);
	}

	/***
	 * 伪装一个非事务producer，简化调用
	 * 因为ons每个topic只能有一个producer，且事务和非事务producer不能混用
	 * @author wayshall
	 * @return
	 */
	public ProducerService fakeProducerService(){
		return fakeProducerService;
	}
	
	public static class FakeProducerService implements ProducerService {
		@Autowired
		private TransactionProducerService transactionProducerService;

		@Override
		public void sendMessage(String topic, String tags, Object body) {
			SimpleMessage message = SimpleMessage.builder()
												 .topic(topic)
												 .tags(tags)
												 .body(body)
												 .build();
			this.sendMessage(message);
		}

		@Override
		public SendResult sendMessage(OnsMessage onsMessage) {
			SendResult result = transactionProducerService.sendMessage(onsMessage, COMMIT_EXECUTER, null);
			return result;
		}

		@Override
		public <T> T getRawProducer(Class<T> targetClass) {
			return transactionProducerService.getRawProducer(targetClass);
		}

		@Override
		public boolean isTransactional() {
			return transactionProducerService.isTransactional();
		}
		
	}
	
	/*protected class ProducerServiceFactory implements ObjectFactory<ProducerService> {

		@Override
		public ProducerService getObject() throws BeansException {
			return fakeProducerService();
		}

	}*/
}
