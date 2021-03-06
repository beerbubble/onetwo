package org.onetwo.ext.ons;

import org.onetwo.boot.mq.DatabaseTransactionMessageInterceptor;
import org.onetwo.boot.mq.MQProperties;
import org.onetwo.boot.mq.MQProperties.SendMode;
import org.onetwo.boot.mq.MQTransactionalConfiguration;
import org.onetwo.boot.mq.SendMessageRepository;
import org.onetwo.ext.alimq.MessageDeserializer;
import org.onetwo.ext.alimq.MessageSerializer;
import org.onetwo.ext.ons.consumer.DelegateMessageService;
import org.onetwo.ext.ons.consumer.ONSPushConsumerStarter;
import org.onetwo.ext.ons.producer.OnsDatabaseTransactionMessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author wayshall
 * <br/>
 */
@Configuration
@EnableConfigurationProperties({MQProperties.class, ONSProperties.class})
@Import(MQTransactionalConfiguration.class)
public class ONSConfiguration {
	@Autowired
	private ONSProperties onsProperties;
	@Autowired
	private MQProperties mqProperties;
	
	@Bean
	public ONSPushConsumerStarter onsPushConsumerStarter(DelegateMessageService delegateMessageService){
		ONSPushConsumerStarter starter = new ONSPushConsumerStarter();
		starter.setOnsProperties(onsProperties);
		starter.setDelegateMessageService(delegateMessageService);
		return starter;
	}
	
	@Bean
	public DelegateMessageService delegateMessageService(MessageDeserializer messageDeserializer){
		DelegateMessageService delegateMessageService = new DelegateMessageService(messageDeserializer, onsConsumerListenerComposite());
		return delegateMessageService;
	}
	
	@Bean
	public ONSConsumerListenerComposite onsConsumerListenerComposite(){
		return new ONSConsumerListenerComposite();
	}
	
	@Bean
	@ConditionalOnMissingBean(MessageDeserializer.class)
	public MessageDeserializer onsMessageDeserializer(){
		return onsProperties.getSerializer().getDeserializer();
	}
	
	@Bean
	@ConditionalOnMissingBean(MessageSerializer.class)
	public MessageSerializer onsMessageSerializer(){
		return onsProperties.getSerializer().getSerializer();
	}
	
	
	@Bean
	@ConditionalOnMissingBean(SendMessageLogInterceptor.class)
	public SendMessageLogInterceptor sendMessageLogInterceptor(){
		return new SendMessageLogInterceptor();
	}
	

	@Bean
	@ConditionalOnProperty(MQProperties.TRANSACTIONAL_ENABLED_KEY)
	public DatabaseTransactionMessageInterceptor databaseTransactionMessageInterceptor(SendMessageRepository sendMessageRepository){
		OnsDatabaseTransactionMessageInterceptor interceptor = new OnsDatabaseTransactionMessageInterceptor();
		SendMode sendMode = mqProperties.getTransactional().getSendMode();
		if(sendMode==SendMode.ASYNC){
			interceptor.setUseAsync(true);
		}else{
			interceptor.setUseAsync(false);
		}
		interceptor.setSendMessageRepository(sendMessageRepository);
		return interceptor;
	}
	
	/*@Configuration
	@ConditionalOnProperty(ONSProperties.TRANSACTIONAL_ENABLED_KEY)
	@EnableScheduling
	protected static class TransactionalConfiguration {
		@Autowired
		private ONSProperties onsProperties;

		@Bean
		@ConditionalOnMissingBean(ONSDatabaseTransactionMessageInterceptor.class)
		public DatabaseTransactionMessageInterceptor databaseTransactionMessageInterceptor(SendMessageRepository sendMessageRepository){
			ONSDatabaseTransactionMessageInterceptor interceptor = new ONSDatabaseTransactionMessageInterceptor();
			SendMode sendMode = onsProperties.getTransactional().getSendMode();
			if(sendMode==SendMode.ASYNC){
				interceptor.setUseAsync(true);
			}else{
				interceptor.setUseAsync(false);
			}
			interceptor.setSendMessageRepository(sendMessageRepository);
			return interceptor;
		}
		
		@Bean
		@ConditionalOnMissingBean(SendMessageRepository.class)
		public SendMessageRepository sendMessageRepository(){
			return new DbmSendMessageRepository();
		}
		
		@Bean
		@ConditionalOnMissingBean(MessageBodyStoreSerializer.class)
		public MessageBodyStoreSerializer<Message> messageBodyStoreSerializer(){
			return ONSMessageBodyStoreSerializer.INSTANCE;
		}
		
		@Bean
		@ConditionalOnProperty(value=ONSProperties.TRANSACTIONAL_SEND_TASK_ENABLED_KEY, matchIfMissing=false)
		public CompensationSendMessageTask compensationSendMessageTask(){
			CompensationSendMessageTask task = new CompensationSendMessageTask();
			TaskLocks taskLock = onsProperties.getTransactional().getSendTask().getLock();
			if(taskLock==TaskLocks.REDIS){
				task.setUseReidsLock(true);
			}
			return task;
		}
		
		@Bean
		@ConditionalOnProperty(value=ONSProperties.TRANSACTIONAL_DELETE_TASK_ENABLED_KEY, matchIfMissing=false)
		public DeleteSentMessageTask deleteSentMessageTask(){
			DeleteSentMessageTask task = new DeleteSentMessageTask();
			DeleteTaskProps deleteProps = onsProperties.getTransactional().getDeleteTask();
			if(deleteProps.getLock()==TaskLocks.REDIS){
				task.setUseReidsLock(true);
			}
			task.setDeleteBeforeAt(deleteProps.getDeleteBeforeAt());
			return task;
		}
		
	}*/
	
}
