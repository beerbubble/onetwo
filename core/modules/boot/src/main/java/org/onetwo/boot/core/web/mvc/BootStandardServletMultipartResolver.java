package org.onetwo.boot.core.web.mvc;

import javax.servlet.http.HttpServletRequest;

import org.onetwo.common.file.FileUtils;
import org.onetwo.common.web.utils.RequestUtils;
import org.springframework.boot.autoconfigure.web.MultipartProperties;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/***
 * 增加最大上传限制
 * @author way
 *
 */
public class BootStandardServletMultipartResolver extends StandardServletMultipartResolver {
	//10MB
	static private String DEFAULT_MULTIPART_MAX_REQUEST_SIZE = new MultipartProperties().getMaxRequestSize();
	
	public static boolean isBootDefaultValue(String maxFileSize){
		return DEFAULT_MULTIPART_MAX_REQUEST_SIZE.equals(maxFileSize);
	}
	
	public static final int DEFAULT_MAX_UPLOAD_SIZE = FileUtils.parseSize(DEFAULT_MULTIPART_MAX_REQUEST_SIZE);
	
	private int maxUploadSize = DEFAULT_MAX_UPLOAD_SIZE;


	@Override
	public boolean isMultipart(HttpServletRequest request) {
		// fixed StandardServletMultipartResolver
		String method = request.getMethod().toLowerCase();
		if (!"post".equals(method) && !"put".equals(method)) {
			return false;
		}
		String contentType = request.getContentType();
		return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
	}
	
	@Override
	public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
		if(maxUploadSize>=0){
			long requestSize = RequestUtils.getContentLength(request);
			if(requestSize!=-1 && requestSize>maxUploadSize){
				throw new MaxUploadSizeExceededException(maxUploadSize);
			}
		}
		return super.resolveMultipart(request);
	}

	public void setMaxUploadSize(int maxUploadSize) {
		this.maxUploadSize = maxUploadSize;
	}

   
}
