package org.onetwo.common.apiclient.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.onetwo.common.apiclient.CustomResponseHandler;

/**
 * produces -> acceptHeader
 * consumes -> contentType
 * @author wayshall
 * <br/>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ResponseHandler {
	
	Class<? extends CustomResponseHandler<?>> value();
}
