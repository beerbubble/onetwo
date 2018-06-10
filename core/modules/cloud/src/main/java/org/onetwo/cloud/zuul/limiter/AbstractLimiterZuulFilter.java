package org.onetwo.cloud.zuul.limiter;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.onetwo.boot.limiter.InvokeContext;
import org.onetwo.boot.limiter.InvokeContext.DefaultInvokeContext;
import org.onetwo.boot.limiter.InvokeContext.InvokeType;
import org.onetwo.boot.limiter.InvokeLimiter;
import org.onetwo.boot.module.oauth2.clientdetails.Oauth2ClientDetailManager;
import org.onetwo.cloud.zuul.ZuulUtils;
import org.onetwo.common.log.JFishLoggerFactory;
import org.onetwo.common.web.utils.RequestUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;


/**
 * @author wayshall
 * <br/>
 */
abstract public class AbstractLimiterZuulFilter extends ZuulFilter implements InitializingBean {
	
	protected final Logger logger = JFishLoggerFactory.getLogger(this.getClass());

	@Autowired(required=false)
	protected Oauth2ClientDetailManager oauth2ClientDetailManager;
	@Autowired
	private RouteLocator routeLocator;
	private List<InvokeLimiter> limiters;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(routeLocator, "routeLocator cant not be null");
		Assert.notNull(limiters, "limiters can not be null");
		if(!limiters.isEmpty()){
			AnnotationAwareOrderComparator.sort(limiters);
		}
	}
	
	@Override
	public Object run() {
		InvokeContext invokeContext = createInvokeContext();
		
		for(InvokeLimiter limiter : limiters){
			if(limiter.match(invokeContext)){
				limiter.consume(invokeContext);
			}
		}
		
		return null;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}
	
	protected String getRequestPath(){
		HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
		String path = RequestUtils.getServletPath(request);
		return path;
	}

	protected InvokeContext createInvokeContext() {
        final RequestContext ctx = RequestContext.getCurrentContext();
//        final HttpServletResponse response = ctx.getResponse();
        final HttpServletRequest request = ctx.getRequest();
        
		String requestPath = RequestUtils.getUrlPathHelper().getLookupPathForRequest(request);
		String clientIp = RequestUtils.getRemoteAddr(request);
//		ZuulUtils.getRoute(routeLocator, request);
		DefaultInvokeContext invokeContext = DefaultInvokeContext.builder()
														  .invokeType(InvokeType.BEFORE)
														  .requestPath(requestPath)
														  .clientIp(clientIp)
//														  .serviceId(serviceId)
														  .clientUser(null)
														  .invokeTimes(1)
														  .build();
		if(oauth2ClientDetailManager!=null){
			oauth2ClientDetailManager.getCurrentClientDetail().ifPresent(clientDetail->{
				invokeContext.setClientId(clientDetail.getClientId());
			});
		}

//		String serviceId = (String) ctx.get(FilterConstants.SERVICE_ID_KEY);
		ZuulUtils.getRoute(routeLocator, request).ifPresent(route->{
			invokeContext.setServiceId(route.getId());
		});
		
		return invokeContext;
	}

	public void setLimiters(List<InvokeLimiter> limiters) {
		this.limiters = limiters;
	}

}
