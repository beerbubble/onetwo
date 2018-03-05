package org.onetwo.boot.mq;

import java.util.Iterator;
import java.util.List;

import org.onetwo.boot.mq.SendMessageInterceptor.InterceptorPredicate;
import org.onetwo.common.log.JFishLoggerFactory;
import org.onetwo.common.utils.LangUtils;
import org.slf4j.Logger;

abstract public class SendMessageInterceptorChain {
//	private final Logger logger = JFishLoggerFactory.getLogger(this.getClass());

	final private List<SendMessageInterceptor> interceptors;
	final private Iterator<SendMessageInterceptor> iterator;
//	private SendMessageContext<?> sendMessageContext;
	final private InterceptorPredicate interceptorPredicate;
	
	private Object result;
	private Throwable throwable;
	private boolean debug = true;
	private int currentIndex = 0;
	private StringBuilder interLog = new StringBuilder(100);

	public SendMessageInterceptorChain(List<SendMessageInterceptor> interceptors, InterceptorPredicate interceptorPredicate) {
		super();
		this.interceptors = interceptors;
		this.iterator = this.interceptors.iterator();
		this.interceptorPredicate = interceptorPredicate;
	}

	public Object invoke(){
		if(iterator.hasNext()){
			SendMessageInterceptor interceptor = iterator.next();
			if(interceptorPredicate==null || interceptorPredicate.isApply(interceptor)){
				if(debug){
//					logger.info("{}-> {}", LangUtils.repeatString(index, "--"), interceptor.getClass().getSimpleName());
					interLog.append(LangUtils.repeatString(currentIndex, "--")).append("-> ").append(interceptor.getClass().getSimpleName()).append('\n');
				}
				result = interceptor.intercept(this);
				currentIndex++;
			}else{
				result = this.invoke();
			}
		}else{
//			result = actualInvoker.get();
			Logger logger = getLogger();
			if(interLog.length()>0 && logger.isInfoEnabled()){
				logger.info("SendMessageInterceptors chain:\n{}", interLog.toString());
			}
			result = doSendRawMessage();
		}
		return result;
	}

	protected Logger getLogger(){
		return JFishLoggerFactory.getLogger(this.getClass());
	}
	abstract protected Object doSendRawMessage();
	abstract public SendMessageContext getSendMessageContext();

	
	public Object getResult() {
		return result;
	}
	
	public Throwable getThrowable() {
		return throwable;
	}

	/*public SendMessageContext<?> getSendMessageContext() {
		return sendMessageContext;
	}

	void setSendMessageContext(SendMessageContext<?> sendMessageContext) {
		this.sendMessageContext = sendMessageContext;
	}*/

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

}
