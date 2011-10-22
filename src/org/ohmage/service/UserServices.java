package org.ohmage.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jbcrypt.BCrypt;

import org.ohmage.annotator.Annotator.ErrorCode;
import org.ohmage.domain.Clazz;
import org.ohmage.domain.UserInformation;
import org.ohmage.domain.UserPersonal;
import org.ohmage.domain.campaign.Campaign;
import org.ohmage.exception.DataAccessException;
import org.ohmage.exception.ServiceException;
import org.ohmage.query.impl.UserCampaignQueries;
import org.ohmage.query.impl.UserClassQueries;
import org.ohmage.query.impl.UserQueries;

/**
 * This class contains the services for users.
 * 
 * @author John Jenkins
 */
public final class UserServices {
	/**
	 * Default constructor. Made private so that it cannot be instantiated.
	 */
	private UserServices() {}
	
	/**
	 * Creates a new user.
	 * 
	 * @param username The username for the new user.
	 * 
	 * @param password The password for the new user.
	 * 
	 * @param admin Whether or not the user should initially be an admin.
	 * 
	 * @param enabled Whether or not the user should initially be enabled.
	 * 
	 * @param newAccount Whether or not the new user must change their password
	 * 					 before using any other APIs.
	 * 
	 * @param campaignCreationPrivilege Whether or not the new user is allowed
	 * 									to create campaigns.
	 * 
	 * @throws ServiceException Thrown if there is an error.
	 */
	public static void createUser(final String username, final String password, 
			final Boolean admin, final Boolean enabled, 
			final Boolean newAccount, final Boolean campaignCreationPrivilege)
			throws ServiceException {
		
		try {
			String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(13));
			
			UserQueries.createUser(username, hashedPassword, admin, enabled, newAccount, campaignCreationPrivilege);
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Checks that a user's existence matches that of 'shouldExist'.
	 * 
	 * @param username The username of the user in question.
	 * 
	 * @param shouldExist Whether or not the user should exist.
	 * 
	 * @throws ServiceException Thrown if there was an error, if the user 
	 * 							exists but shouldn't, or if the user doesn't
	 * 							exist but should.
	 */
	public static void checkUserExistance(final String username, 
			final boolean shouldExist) throws ServiceException {
		
		try {
			if(UserQueries.userExists(username)) {
				if(! shouldExist) {
					throw new ServiceException(
							ErrorCode.USER_INVALID_USERNAME, 
							"The following user already exists: " + username);
				}
			}
			else {
				if(shouldExist) {
					throw new ServiceException(
							ErrorCode.USER_INVALID_USERNAME, 
							"The following user does not exist: " + username);
				}
			}
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Checks that a Collection of users' existence matches that of 
	 * 'shouldExist'.
	 * 
	 * @param usernames A Collection of usernames to check that each exists.
	 * 
	 * @param shouldExist Whether or not all of the users should exist or not.
	 * 
	 * @throws ServiceException Thrown if there was an error, if one of the 
	 * 							users should have existed and didn't, or if one 
	 * 							of the users shouldn't exist but does.
	 */
	public static void verifyUsersExist(final Collection<String> usernames, 
			final boolean shouldExist) throws ServiceException {
		
		for(String username : usernames) {
			checkUserExistance(username, shouldExist);
		}
	}
	
	/**
	 * Checks if the user is an admin.
	 * 
	 * @return Returns true if the user is an admin; false if not or there is
	 * 		   an error.
	 * 
	 * @throws ServiceException Thrown if there was an error or if the user is
	 * 							not an admin.
	 */
	public static void verifyUserIsAdmin(final String username) 
			throws ServiceException {
		
		try {
			if(! UserQueries.userIsAdmin(username)) {
				throw new ServiceException(
						ErrorCode.USER_INSUFFICIENT_PERMISSIONS, 
						"The user is not an admin."
					);
			}
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Verifies that the user can create campaigns.
	 * 
	 * @param username The username of the user whose campaign creation ability
	 * 				   is being checked.
	 * 
	 * @throws ServiceException Thrown if there is an error or if the user is
	 * 							not allowed to create campaigns.
	 */
	public static void verifyUserCanCreateCampaigns(final String username) 
			throws ServiceException {
		
		try {
			if(! UserQueries.userCanCreateCampaigns(username)) {
				throw new ServiceException(
						ErrorCode.CAMPAIGN_INSUFFICIENT_PERMISSIONS, 
						"The user does not have permission to create new campaigns.");
			}
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Verifies that a given user is allowed to read the personal information
	 * about a group of users.
	 * 
	 * @param username The username of the reader.
	 * 
	 * @param usernames The usernames of the readees.
	 * 
	 * @throws ServiceException There was an error or the user is not allowed 
	 * 							to read the personal information of one or more
	 * 							of the users.
	 */
	public static void verifyUserCanReadUsersPersonalInfo(
			final String username, final Collection<String> usernames) 
			throws ServiceException {
		
		if((usernames == null) || (usernames.size() == 0) ||
				((usernames.size() == 1) && 
				 usernames.iterator().next().equals(username))) {
			return;
		}
		
		Set<String> supervisorCampaigns = 
			UserCampaignServices.getCampaignsForUser(username, 
					null, null, null, null, null, null, 
					Campaign.Role.SUPERVISOR);
		
		Set<String> privilegedClasses = 
			UserClassServices.getClassesForUser(
					username, 
					Clazz.Role.PRIVILEGED);
		
		for(String currUsername : usernames) {
			if(UserCampaignServices.getCampaignsForUser( 
					currUsername, supervisorCampaigns, privilegedClasses, 
					null, null, null, null, null).size() == 0) {
				
				throw new ServiceException(
						ErrorCode.USER_INSUFFICIENT_PERMISSIONS, 
						"The user is not allowed to view personal information about a user in the list: " + 
							currUsername);
			}
		}
	}

	/**
	 * Verifies that if the 'personalInfo' is not null nor empty, that either
	 * there already exists a personal information entry for some user or that
	 * there is sufficient information in the 'personalInfo' object to create a
	 * new entry.
	 * 
	 * @param username The username of the user whose personal information is
	 * 				   being queried.
	 * 
	 * @param personalInfo The personal information to use to populate the 
	 * 					   user's personal information entry in the database
	 * 					   should one not exist.
	 * 
	 * @throws ServiceException Thrown if the 'personalInfo' object is not null
	 * 							nor is it empty, there is not a personal
	 * 							information entry for this user in the 
	 * 							database, and there is some required field 
	 * 							missing in the 'personalInfo' object to create
	 * 							a new personal information entry in the
	 * 							database. Also, it is thrown if there is an 
	 * 							error. 
	 */
	public static void verifyUserHasOrCanCreatePersonalInfo(
			final String username, final UserPersonal personalInfo) 
			throws ServiceException {
		
		if((personalInfo != null) && (! personalInfo.isEmpty())) {
			try {
				if(! UserQueries.userHasPersonalInfo(username)) {
					if(personalInfo.getFirstName() == null) {
						throw new ServiceException(
								ErrorCode.USER_INVALID_FIRST_NAME_VALUE, 
								"The user doesn't have personal information yet, and a first name is necessary to create one.");
					}
					else if(personalInfo.getLastName() == null) {
						throw new ServiceException(
								ErrorCode.USER_INVALID_LAST_NAME_VALUE, 
								"The user doesn't have personal information yet, and a last name is necessary to create one.");
					}
					else if(personalInfo.getOrganization() == null) {
						throw new ServiceException(
								ErrorCode.USER_INVALID_ORGANIZATION_VALUE, 
								"The user doesn't have personal information yet, and an organization is necessary to create one.");
					}
					else if(personalInfo.getPersonalId() == null) {
						throw new ServiceException(
								ErrorCode.USER_INVALID_PERSONAL_ID_VALUE, 
								"The user doesn't have personal information yet, and a personal ID is necessary to create one.");
					}
				}
			}
			catch(DataAccessException e) {
				throw new ServiceException(e);
			}
		}
	}
	
	/**
	 * Gathers the personal information about a user.
	 * 
	 * @param username The username of the user whose information is being
	 * 				   requested.
	 * 
	 * @return Returns a UserInformation object that contains the necessary
	 * 		   information about a user.
	 * 
	 * @throws ServiceException Thrown if there is an error.
	 */
	public static UserInformation gatherUserInformation(final String username)
			throws ServiceException {
		
		try {
			// Get campaign creation privilege.
			UserInformation userInformation = new UserInformation(UserQueries.userCanCreateCampaigns(username));
			
			// Get the campaigns and their names for the requester.
			Map<String, String> campaigns = UserCampaignQueries.getCampaignIdsAndNameForUser(username);
			userInformation.addCampaigns(campaigns);
			
			// Get the requester's campaign roles for each of the campaigns.
			for(String campaignId : campaigns.keySet()) {
				userInformation.addCampaignRoles(UserCampaignQueries.getUserCampaignRoles(username, campaignId));
			}
			
			// Get the classes and their names for the requester.
			Map<String, String> classes = UserClassQueries.getClassIdsAndNameForUser(username);
			userInformation.addClasses(classes);
			
			// Get the requester's class roles for each of the classes.
			for(String classId : classes.keySet()) {
				userInformation.addClassRole(UserClassQueries.getUserClassRole(classId, username));
			}
			
			return userInformation;
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Retrieves the personal information for all of the users in the list.
	 * 
	 * @param usernames The usernames.
	 * 
	 * @return A map of usernames to personal information or null if no 
	 * 		   personal information is available.
	 * 
	 * @throws ServiceException There was an error.
	 */
	public static Map<String, UserPersonal> gatherPersonalInformation(
			final Collection<String> usernames) throws ServiceException {
		
		try {
			Map<String, UserPersonal> result = 
				new HashMap<String, UserPersonal>(usernames.size());
			
			for(String username : usernames) {
				result.put(username, UserQueries.getPersonalInfoForUser(username));
			}
			
			return result;
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Updates a user's account information.
	 * 
	 * @param username The username of the user whose information is to be
	 * 				   updated.
	 * 
	 * @param admin Whether or not the user should be an admin. A null value
	 * 			    indicates that this field should not be updated.
	 * 
	 * @param enabled Whether or not the user's account should be enabled. A
	 * 				  null value indicates that this field should not be
	 * 				  updated.
	 * 
	 * @param newAccount Whether or not the user should be required to change
	 * 					 their password. A null value indicates that this field
	 * 					 should not be updated.
	 * 
	 * @param campaignCreationPrivilege Whether or not the user should be 
	 * 									allowed to create campaigns. A null
	 * 									Value indicates that this field should
	 * 									not be updated.
	 * 
	 * @param personalInfo Personal information about a user. If this is null,
	 * 					   none of the user's personal information will be
	 * 					   updated. If it is not null, all non-null values 
	 * 					   inside this object will be used to update the user's
	 * 					   personal information database record; all null 
	 * 					   values will be ignored.
	 * 
	 * @throws ServiceException Thrown if there is an error.
	 */
	public static void updateUser(final String username, final Boolean admin, 
			final Boolean enabled, final Boolean newAccount, 
			final Boolean campaignCreationPrivilege, 
			final UserPersonal personalInfo) throws ServiceException {
		
		try {
			UserQueries.updateUser(username, admin, enabled, newAccount, campaignCreationPrivilege, personalInfo);
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Updates the user's password.
	 * 
	 * @param username The username of the user whose password is being 
	 * 				   updated.
	 * 
	 * @param plaintextPassword The plaintext password for the user.
	 * 
	 * @throws ServiceException Thrown if there is an error.
	 */
	public static void updatePassword(final String username, 
			final String plaintextPassword) throws ServiceException {
		
		try {
			String hashedPassword = BCrypt.hashpw(plaintextPassword, BCrypt.gensalt(13));
			
			UserQueries.updateUserPassword(username, hashedPassword);
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Deletes all of the users from the Collection.
	 * 
	 * @param usernames A Collection of usernames of the users to delete.
	 * 
	 * @throws ServiceException Thrown if there is an error.
	 */
	public static void deleteUser(final Collection<String> usernames) 
			throws ServiceException {
		
		try {
			UserQueries.deleteUsers(usernames);
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
}