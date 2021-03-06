/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ohmage.annotator.Annotator.ErrorCode;
import org.ohmage.domain.Clazz;
import org.ohmage.exception.DataAccessException;
import org.ohmage.exception.ServiceException;
import org.ohmage.query.IUserClassDocumentQueries;
import org.ohmage.query.IUserClassQueries;


public class UserClassDocumentServices {
	private static UserClassDocumentServices instance;

	private IUserClassQueries userClassQueries;
	private IUserClassDocumentQueries userClassDocumentQueries;
		
	/**
	 * Default constructor. Privately instantiated via dependency injection
	 * (reflection).
	 * 
	 * @throws IllegalStateException if an instance of this class already
	 * exists
	 * 
	 * @throws IllegalArgumentException if iUserClassQueries or
	 * iUserClassDocumentQueries is null
	 */
	private UserClassDocumentServices(IUserClassQueries iUserClassQueries, IUserClassDocumentQueries iUserClassDocumentQueries) {
		if(instance != null) {
			throw new IllegalStateException("An instance of this class already exists.");
		}
		
		if(iUserClassQueries == null) {
			throw new IllegalArgumentException("An instance of IUserClassQueries is required.");
		}
		if(iUserClassDocumentQueries == null) {
			throw new IllegalArgumentException("An instance of IUserClassDocumentQueries is required.");
		}
		
		userClassQueries = iUserClassQueries;
		userClassDocumentQueries = iUserClassDocumentQueries;
		
		instance = this;		
	}
	
	/**
	 * @return  Returns the singleton instance of this class.
	 */
	public static UserClassDocumentServices instance() {
		return instance;
	}
	
	/**
	 * Verifies that the user can associate documents with a class. Currently,
	 * the only restriction is that the user must belong to the class in some
	 * capacity.
	 * 
	 * @param username The username of the user in question.
	 * 
	 * @param classId The class ID of the class in question.
	 * 
	 * @throws ServiceException Thrown if there is an error or if the user 
	 * 							does not belong to the class in any capacity.
	 */
	public void userCanAssociateDocumentsWithClass(
			final String username, final String classId) 
			throws ServiceException {
		
		try {
			Clazz.Role classRole = userClassQueries.getUserClassRole(classId, username);
			
			if(classRole == null) {
				throw new ServiceException(
						ErrorCode.DOCUMENT_INSUFFICIENT_PERMISSIONS, 
						"The user is not a member of the following class and, therefore, cannot associate documents with it: " + 
							classId);
			}
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Verifies that the user can disassociate documents from a class. 
	 * Currently, the only restriction is that the user must belong to the 
	 * class in some capacity.
	 * 
	 * @param username The username of the user in question.
	 * 
	 * @param classId The class ID of the class in question.
	 * 
	 * @throws ServiceException Thrown if there is an error or if the user 
	 * 							does not belong to the class in any capacity.
	 */
	public void userCanDisassociateDocumentsWithClass(
			final String username, final String classId) 
			throws ServiceException {
		
		try {
			Clazz.Role classRole = userClassQueries.getUserClassRole(classId, username);
			
			if(classRole == null) {
				throw new ServiceException(
						ErrorCode.DOCUMENT_INSUFFICIENT_PERMISSIONS, 
						"The user is not a member of the following class and, therefore, cannot disassociate documents from it: " + 
							classId);
			}
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Verifies that the user can associate documents with all of the classes 
	 * in the list. Currently, the only restriction is that the user must 
	 * belong to each of the classes in some capacity.
	 * 
	 * @param username The username of the user in question.
	 * 
	 * @param classIds A List of class IDs where the user must belong to all of
	 * 				   the classes in some capacity.
	 * 				 
	 * @throws ServiceException Thrown if there is an error or if the user 
	 * 							doesn't belong to one or more of the classes.
	 */
	public void userCanAssociateDocumentsWithClasses(
			final String username, final Collection<String> classIds) 
			throws ServiceException {
		
		for(String classId : classIds) {
			userCanAssociateDocumentsWithClass(username, classId);
		}
	}
	
	/**
	 * Verifies that the user can disassociate documents with all of the  
	 * classes in the list. Currently, the only restriction is that the user  
	 * must belong to each of the classes in some capacity.
	 * 
	 * @param username The username of the user in question.
	 * 
	 * @param classIds A List of class IDs where the user must belong to all of
	 * 				   the classes in some capacity.
	 * 				 
	 * @throws ServiceException Thrown if there is an error or if the user 
	 * 							doesn't belong to one or more of the classes.
	 */
	public void userCanDisassociateDocumentsWithClasses(
			final String username, final Collection<String> classIds) 
			throws ServiceException {
		
		for(String classId : classIds) {
			userCanDisassociateDocumentsWithClass(username, classId);
		}
	}
	
	/**
	 * Retrieves a list of all of the documents associated with a class.
	 * 
	 * @param username The username of the requester.
	 * 
	 * @param classId The class' unique identifier.
	 * 
	 * @return A list of all of unique identifiers for all of the documents 
	 * 		   associated with the class. The list may be empty but will never
	 * 		   be null.
	 * 
	 * @throws ServiceException Thrown if there is an error.
	 */
	public List<String> getVisibleDocumentsSpecificToClass(
			final String username, final String classId) 
			throws ServiceException {
		
		try {
			return userClassDocumentQueries.getVisibleDocumentsToUserInClass(username, classId);
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
	
	/**
	 * Retrieves a list of all of the documents associated with all of the 
	 * classes in a collection.
	 * 
	 * @param username The username of the requester.
	 * 
	 * @param classIds A list of unique identifiers of classes.
	 * 
	 * @return A list of the unique identifiers for all of the documents 
	 * 		   associated with the class. This list may be empty but will never
	 * 		   be null and will contain only unique entries.
	 * 
	 * @throws ServiceException Thrown if there is an error.
	 */
	public List<String> getVisibleDocumentsSpecificToClasses(
			final String username, final Collection<String> classIds) 
			throws ServiceException {
		
		Set<String> resultSet = new HashSet<String>();
		for(String classId : classIds) {
			resultSet.addAll(getVisibleDocumentsSpecificToClass(username, classId));
		}
		return new ArrayList<String>(resultSet);
	}
	
	/**
	 * Retrieves whether or not the user is privileged in any class with which
	 * the document is associated.
	 * 
	 * @param username The username of the user.
	 * 
	 * @param documentId The unique identifier for the document.
	 * 
	 * @return Whether or not he user is privileged in any class with which the
	 * 		   document is associated.
	 * 
	 * @throws ServiceException Thrown if there is an error.
	 */
	public boolean getUserIsPrivilegedInAnyClassAssociatedWithDocument(
			final String username, final String documentId) 
			throws ServiceException {
		
		try {
			return userClassDocumentQueries.getUserIsPrivilegedInAnyClassAssociatedWithDocument(username, documentId);
		}
		catch(DataAccessException e) {
			throw new ServiceException(e);
		}
	}
}
