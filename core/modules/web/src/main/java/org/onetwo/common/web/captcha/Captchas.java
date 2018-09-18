package org.onetwo.common.web.captcha;

import lombok.Data;

import org.apache.commons.lang3.RandomStringUtils;
import org.onetwo.common.log.JFishLoggerFactory;
import org.onetwo.common.md.Hashs;
import org.onetwo.common.md.MessageDigestHasher;
import org.slf4j.Logger;
import org.springframework.util.Assert;

/**
 * @author wayshall
 * <br/>
 */
public class Captchas {
//	static public final String DEFAULT_SALT = "__jfish_captchas__";
	/**
	 * 默认失效时间，三分钟
	 */
	static public final int DEFAULT_VALID_IN_SECONDS = 60*3;
	
	static private final CaptchaChecker CAPTCHA_CHECKER = new CaptchaChecker(RandomStringUtils.randomAscii(64), DEFAULT_VALID_IN_SECONDS);

	
	public static CaptchaChecker getCaptchaChecker() {
		return CAPTCHA_CHECKER;
	}
	
	public static CaptchaChecker createCaptchaChecker(String salt, int validInSeconds) {
		return new CaptchaChecker(salt, validInSeconds);
	}

	public static CaptchaSignedResult signCode(String code){
		return CAPTCHA_CHECKER.sign(code);
	}
	
	public static boolean checkCode(String code, String hashStr){
		return CAPTCHA_CHECKER.check(code, hashStr);
	}
	
	
	@Data
	public static class CaptchaChecker {
		final private String salt;
		final private int expireInSeconds;
		final private MessageDigestHasher hasher = Hashs.SHA;

		public CaptchaChecker(String salt, int validInSeconds) {
			super();
			this.salt = salt;
			Assert.isTrue(validInSeconds>0, "validInSeconds["+validInSeconds+"] must greater than 0");
			this.expireInSeconds = validInSeconds;
		}
		
		public boolean check(String code, String hashStr){
			long validTime = getValidTime();
			String source = code+salt+validTime;
			boolean res = hasher.checkHash(source, hashStr);
			return res;
		}
		
		public CaptchaSignedResult sign(String code){
			long validTime = getValidTime();
			Logger logger = JFishLoggerFactory.getCommonLogger();
			if(logger.isInfoEnabled()){
				logger.info("signing validTime: {}", validTime);
			}
			String source = code+salt+validTime;
			String signed = hasher.hash(source);
			return new CaptchaSignedResult(signed, validTime);
		}
		
		protected long getValidTime(){
			//四舍五入，避免接近边界值时，瞬时失效
			long validTime = Math.round(System.currentTimeMillis() / 1000.0 / expireInSeconds);
			return validTime;
		}
	}
	@Data
	public static class CaptchaSignedResult {
		private final String signed;
		private final long validTime;
		public CaptchaSignedResult(String signed, long validTime) {
			super();
			this.signed = signed;
			this.validTime = validTime;
		}
	}
	
	private Captchas(){
	}

}
