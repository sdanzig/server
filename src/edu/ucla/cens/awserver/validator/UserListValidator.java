package edu.ucla.cens.awserver.validator;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.NewDataPointQueryAwRequest;
import edu.ucla.cens.awserver.util.StringUtils;

/**
 * Validates the username list for a new data point query.
 * 
 * @author selsky
 */
public class UserListValidator extends AbstractAnnotatingRegexpValidator {
	private static Logger _logger = Logger.getLogger(UserListValidator.class);
	
	private String _key;
	private boolean _required;
	
	public UserListValidator(String regexp, AwRequestAnnotator awRequestAnnotator, String key, boolean required) {
		super(regexp, awRequestAnnotator);
		
		if(StringUtils.isEmptyOrWhitespaceOnly(key)) {
			throw new IllegalArgumentException("A key is required.");
		}
		
		_key = key;
		_required = required;
	}
	
	/**
	 * @throws ValidatorException if the awRequest is not a NewDataPointQueryAwRequest
	 */
	public boolean validate(AwRequest awRequest) {
		_logger.info("validating user list for key: " + _key);
		
		if(awRequest instanceof NewDataPointQueryAwRequest) { // lame
			// TODO: As a quick fix, I (John) moved this into the if statement
			// and implemented a request-agnostic version in the if portion.
			// Once NewDataPointQueryAwRequest has been modified to use the
			// new toValidate map, this whole section can be removed, and it
			// *should* just work out-of-the-box.
			String userListString = ((NewDataPointQueryAwRequest) awRequest).getUserListString();
			
			// _logger.info(userListString);
			
			if(StringUtils.isEmptyOrWhitespaceOnly(userListString)) {
				
				getAnnotator().annotate(awRequest, "empty user name list found");
				return false;
			
			}
			
			// first check for the special "all users" value
			// FIXME: This should be defined somewhere as a constant rather
			// than replicated everywhere.
			if("urn:ohmage:special:all".equals(userListString)) {
				
				return true;
				
			} else {
				
				String[] users = userListString.split(",");
				
				if(users.length > 10) {
					
					getAnnotator().annotate(awRequest, "more than 10 users in query: " + userListString);
					return false;
					
				} else {
					
					for(int i = 0; i < users.length; i++) {
						if(! _regexpPattern.matcher(users[i]).matches()) {
							getAnnotator().annotate(awRequest, "incorrect user name: " + users[i]);
							return false;
						}
					}
				}
			}
			
			return true;
		}
		else { // The generalized solution.
			String userList = (String) awRequest.getToValidate().get(_key);
			
			if(userList == null) {
				if(_required) {
					_logger.error("Missing " + _key + " in request. This should have been caught earlier.");
					throw new ValidatorException("Missing key in request: " + _key);
				}
				else {
					return true;
				}
			}
			
			if("urn:ohmage:special:all".equals(userList)) {
				awRequest.addToProcess(_key, "urn:ohmage:special:all", true);
				return true;
			}
			else {
				String[] users = userList.split(",");
				
				for(int i = 0; i < users.length; i++) {
					if(! _regexpPattern.matcher(users[i]).matches()) {
						getAnnotator().annotate(awRequest, "Invalid user name: " + users[i]);
						awRequest.setFailedRequest(true);
						return false;
					}
				}
			}
			
			awRequest.addToProcess(_key, userList, true);
			return true;
		}
	}
}
