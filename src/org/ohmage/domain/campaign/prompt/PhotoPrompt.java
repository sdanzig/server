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
package org.ohmage.domain.campaign.prompt;

import java.io.IOException;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.config.grammar.custom.ConditionValuePair;
import org.ohmage.domain.campaign.Prompt;
import org.ohmage.domain.campaign.PromptResponse;
import org.ohmage.domain.campaign.Response.NoResponse;
import org.ohmage.domain.campaign.response.PhotoPromptResponse;
import org.ohmage.exception.DomainException;

/**
 * This class represents a photo prompt.
 * 
 * @author John Jenkins
 */
public class PhotoPrompt extends Prompt {
	/**
	 * <p>
	 * Special NoRepsonse values for images.
	 * </p>
	 *
	 * @author John Jenkins
	 */
	public static enum NoResponseMedia {
		/**
		 * The image was not uploaded.
		 */
		MEDIA_NOT_UPLOADED;
		
		@Override
		public String toString() {
			return name();
		}
	}
	
	private static final String JSON_KEY_MAXIMUM_DIMENSION = "max_dimension";
	
	/**
	 * The campaign configuration property key for the maximum allowed
	 * dimension for a photo.
	 */
	public static final String XML_KEY_MAXIMUM_DIMENSION = "maxDimension";
	
	private final Integer maximumDimension;
	
	/**
	 * Creates a photo prompt.
	 * 
	 * @param id The unique identifier for the prompt within its survey item
	 * 			 group.
	 * 
	 * @param condition The condition determining if this prompt should be
	 * 					displayed.
	 * 
	 * @param unit The unit value for this prompt.
	 * 
	 * @param text The text to be displayed to the user for this prompt.
	 * 
	 * @param explanationText A more-verbose version of the text to be 
	 * 						  displayed to the user for this prompt.
	 * 
	 * @param skippable Whether or not this prompt may be skipped.
	 * 
	 * @param skipLabel The text to show to the user indicating that the prompt
	 * 					may be skipped.
	 * 
	 * @param displayLabel The display label for this prompt.
	 * 
	 * @param maximumDimension The maximum allowed dimension for a photo.
	 * 
	 * @param index This prompt's index in its container's list of survey 
	 * 				items.
	 * 
	 * @throws DomainException Thrown if the vertical resolution is negative.
	 */
	public PhotoPrompt(
			final String id, 
			final String condition, 
			final String unit, 
			final String text, 
			final String explanationText,
			final boolean skippable, 
			final String skipLabel,
			final String displayLabel,
			final Integer maximumDimension,
			final int index) 
			throws DomainException {
		
		super(
			id,
			condition,
			unit,
			text,
			explanationText,
			skippable,
			skipLabel,
			displayLabel,
			Type.PHOTO,
			index);
		
		if((maximumDimension != null) && (maximumDimension < 0)) {
			throw new DomainException(
					"The maximum dimension cannot be negative.");
		}
		
		this.maximumDimension = maximumDimension;
	}
	
	/**
	 * Returns the maximum allowed dimension of an image.
	 * 
	 * @return The maximum allowed dimension of an image or null if it
	 * 		   was not given.
	 */
	public Integer getMaximumDimension() {
		return maximumDimension;
	}
	
	/**
	 * Conditions are not allowed for photo prompts unless they are
	 * {@link NoResponse} values.
	 * 
	 * @param pair The pair to validate.
	 * 
	 * @throws DomainException Always thrown because conditions are not allowed
	 * 						   for photo prompts.
	 */
	@Override
	public void validateConditionValuePair(
			final ConditionValuePair pair)
			throws DomainException {
		
		throw
			new DomainException(
				"Conditions are not allowed for photo prompts.");
	}

	/**
	 * Validates that a given value is valid and, if so, converts it into an
	 * appropriate object.
	 * 
	 * @param value The value to be validated. This must be one of the  
	 * 				following:<br />
	 * 				<ul>
	 * 				<li>{@link NoResponse}</li>
	 * 				<li>{@link UUID}</li>
	 * 				<li>{@link String} that represents:</li>
	 * 				  <ul>
	 * 				    <li>{@link NoResponse}</li>
	 * 				    <li>{@link UUID}</li>
	 * 				  <ul>
	 * 				</ul>
	 * 
	 * @return A {@link UUID} object or a {@link NoResponse} object.
	 * 
	 * @throws DomainException The value is invalid.
	 */
	@Override
	public Object validateValue(final Object value) throws DomainException {
		// If it's already a NoResponse value, then return make sure that if it
		// was skipped that it as skippable.
		if(value instanceof NoResponse) {
			if(NoResponse.SKIPPED.equals(value) && (! skippable())) {
				throw new DomainException(
						"The prompt, '" +
							getId() +
							"', was skipped, but it is not skippable.");
			}
			
			return value;
		}
		// If it is already a UUID value, then return it.
		else if(value instanceof UUID) {
			return value;
		}
		// If it is a String value then attempt to decode it into a NoResponse
		// value or a UUID value.
		else if(value instanceof String) {
			String valueString = (String) value;
			
			try {
				return NoResponse.valueOf(valueString);
			}
			catch(IllegalArgumentException notNoResponse) {
				try {
					return NoResponseMedia.valueOf(valueString);
				}
				catch(IllegalArgumentException noImageNoResponse) {
					try {
						return UUID.fromString(valueString);
					}
					catch(IllegalArgumentException notUuid) {
						throw new DomainException(
								"The string response value was not " +
									"decodable into a UUID for prompt '" +
									getId() +
									"': " +
									valueString);
					}
				}
			}
		}
		else {
			throw new DomainException(
					"The value is not decodable as a reponse value for prompt '" +
						getId() + 
						"'.");
		}
	}
	
	/**
	 * Creates a response to this prompt based on a response value.
	 * 
	 * @param response The response from the user as an Object.
	 * 
	 * @param repeatableSetIteration If this prompt belongs to a repeatable 
	 * 								 set, this is the iteration of that 
	 * 								 repeatable set on which the response to
	 * 								 this prompt was made.
	 * 
	 * @throws DomainException Thrown if this prompt is part of a repeatable 
	 * 						   set but the repeatable set iteration value is 
	 * 						   null, if the repeatable set iteration value is 
	 * 						   negative, or if the value is not a valid 
	 * 						   response value for this prompt.
	 */
	@Override
	public PhotoPromptResponse createResponse(
			final Integer repeatableSetIteration,
			final Object response) 
			throws DomainException {
		
		return new PhotoPromptResponse(
				this,
				repeatableSetIteration,
				response);
	}
	
	/**
	 * Creates a JSONObject that represents this photo prompt.
	 * 
	 * @return A JSONObject that represents this photo prompt.
	 * 
	 * @throws JSONException There was a problem creating the JSONObject.
	 */
	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject result = super.toJson();
		
		if(maximumDimension != null) {
			result.put(JSON_KEY_MAXIMUM_DIMENSION, maximumDimension);
		}
		
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.domain.campaign.SurveyItem#toConcordia(org.codehaus.jackson.JsonGenerator)
	 */
	@Override
	public void toConcordia(
			final JsonGenerator generator)
			throws JsonGenerationException, IOException {
		
		// The response is always an object.
		generator.writeStartObject();
		generator.writeStringField("type", "object");
		
		// The fields array.
		generator.writeArrayFieldStart("schema");
		
		// The first field in the object is the prompt's ID.
		generator.writeStartObject();
		generator.writeStringField("name", PromptResponse.JSON_KEY_PROMPT_ID);
		generator.writeStringField("type", "string");
		generator.writeEndObject();
		
		// The second field in the object is the response's value.
		generator.writeStartObject();
		generator.writeStringField("name", PromptResponse.JSON_KEY_RESPONSE);
		generator.writeStringField("type", "string");
		generator.writeEndObject();
		
		// End the array of fields.
		generator.writeEndArray();
		
		// End the object.
		generator.writeEndObject();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result =
			prime *
				result +
				((maximumDimension == null) ? 0 : maximumDimension.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		if(!super.equals(obj)) {
			return false;
		}
		if(!(obj instanceof PhotoPrompt)) {
			return false;
		}
		PhotoPrompt other = (PhotoPrompt) obj;
		if(maximumDimension == null) {
			if(other.maximumDimension != null) {
				return false;
			}
		}
		else if(!maximumDimension.equals(other.maximumDimension)) {
			return false;
		}
		return true;
	}
}