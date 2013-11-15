package org.ohmage.domain.survey;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import name.jenkins.paul.john.concordia.Concordia;
import name.jenkins.paul.john.concordia.exception.ConcordiaException;
import name.jenkins.paul.john.concordia.schema.ObjectSchema;

import org.ohmage.domain.MetaData;
import org.ohmage.domain.Schema;
import org.ohmage.domain.exception.InvalidArgumentException;
import org.ohmage.domain.survey.response.Respondable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>
 * A survey definition.
 * </p>
 *
 * @author John Jenkins
 */
public class Survey extends Schema {
    /**
     * <p>
     * A builder for a {@link Survey}.
     * </p>
     *
     * @author John Jenkins
     */
    public static class Builder extends Schema.Builder {
        /**
         * The list of survey items.
         */
        protected List<SurveyItem> surveyItems;

        /**
         * Creates a new Survey builder object.
         *
         * @param version
         *        The version of this schema.
         *
         * @param name
         *        The name of this schema.
         *
         * @param description
         *        The description of this schema.
         *
         * @param surveyItems
         *        The survey items that define this survey.
         */
        @JsonCreator
        public Builder(
            @JsonProperty(JSON_KEY_VERSION) final long version,
            @JsonProperty(JSON_KEY_NAME) final String name,
            @JsonProperty(JSON_KEY_DESCRIPTION) final String description,
            @JsonProperty(JSON_KEY_SURVEY_ITEMS)
                final List<SurveyItem> surveyItems) {

            super(version, name, description);

            this.surveyItems = surveyItems;
        }

        /**
         * Creates a new builder based on an existing Survey object.
         *
         * @param survey
         *        The existing Survey object on which this Builder should be
         *        based.
         */
        public Builder(final Survey survey) {
            super(survey);

            surveyItems = survey.surveyItems;
        }

        /**
         * Creates a new Survey object from the state of this builder.
         *
         * @throws InvalidArgumentException
         *         The state of this builder is insufficient to build a new
         *         {@link Survey} object.
         */
        @Override
        public Survey build() throws InvalidArgumentException {
            return
                new Survey(
                    (schemaId == null) ? getRandomId() : schemaId,
                    version,
                    name,
                    description,
                    owner,
                    surveyItems,
                    internalReadVersion,
                    internalWriteVersion);
        }
    }

    /**
     * The JSON key for the list of survey items.
     */
    public static final String JSON_KEY_SURVEY_ITEMS = "survey_items";

    /**
     * The list of survey items.
     */
    @JsonProperty(JSON_KEY_SURVEY_ITEMS)
    private final List<SurveyItem> surveyItems;

    /**
     * Creates a new Survey object.
     *
     * @param version
     *        The version of this survey.
     *
     * @param name
     *        The name of this survey.
     *
     * @param description
     *        The description of this survey.
     *
     * @param owner
     *        The owner of this survey.
     *
     * @param surveyItems
     *        The ordered list of survey items that compose this survey.
     *
     * @throws InvalidArgumentException
     *         A parameter is invalid.
     */
    public Survey(
        final long version,
        final String name,
        final String description,
        final String owner,
        final List<SurveyItem> surveyItems)
        throws InvalidArgumentException {

        this(
            getRandomId(),
            version,
            name,
            description,
            owner,
            surveyItems,
            null);
    }

    /**
     * Rebuild an existing Survey object.
     *
     * @param id
     *        The unique identifier for this object. If null, a default value
     *        is given.
     *
     * @param version
     *        The version of this survey.
     *
     * @param name
     *        The name of this survey.
     *
     * @param description
     *        The description of this survey.
     *
     * @param owner
     *        The owner of this survey.
     *
     * @param surveyItems
     *        The ordered list of survey items that compose this survey.
     *
     * @param internalVersion
     *        The internal version of this survey.
     *
     * @throws IllegalArgumentException
     *         The ID is invalid.
     *
     * @throws InvalidArgumentException
     *         A parameter is invalid.
     */
    @JsonCreator
    protected Survey(
        @JsonProperty(JSON_KEY_ID) final String id,
        @JsonProperty(JSON_KEY_VERSION) final long version,
        @JsonProperty(JSON_KEY_NAME) final String name,
        @JsonProperty(JSON_KEY_DESCRIPTION) final String description,
        @JsonProperty(JSON_KEY_OWNER) final String owner,
        @JsonProperty(JSON_KEY_SURVEY_ITEMS)
            final List<SurveyItem> surveyItems,
        @JsonProperty(JSON_KEY_INTERNAL_VERSION) final Long internalVersion)
        throws IllegalArgumentException, InvalidArgumentException {

        this(
            id,
            version,
            name,
            description,
            owner,
            surveyItems,
            internalVersion,
            internalVersion);
    }

    /**
     * Builds the Survey object.
     *
     * @param id
     *        The unique identifier for this object.
     *
     * @param version
     *        The version of this survey.
     *
     * @param name
     *        The name of this survey.
     *
     * @param description
     *        The description of this survey.
     *
     * @param owner
     *        The owner of this survey.
     *
     * @param surveyItems
     *        The ordered list of survey items that compose this survey.
     *
     * @param internalReadVersion
     *        The internal version of this survey when it was read from the
     *        database.
     *
     * @param internalWriteVersion
     *        The new internal version of this survey when it will be written
     *        back to the database.
     *
     * @throws IllegalArgumentException
     *         The ID is invalid.
     *
     * @throws InvalidArgumentException
     *         A parameter is invalid.
     */
    private Survey(
        final String id,
        final long version,
        final String name,
        final String description,
        final String owner,
        final List<SurveyItem> surveyItems,
        final Long internalReadVersion,
        final Long internalWriteVersion)
        throws IllegalArgumentException, InvalidArgumentException {

        super(
            id,
            version,
            name,
            description,
            owner,
            internalReadVersion,
            internalWriteVersion);

        if(surveyItems == null) {
            throw
                new InvalidArgumentException(
                    "The list of survey items is null.");
        }

        this.surveyItems = surveyItems;
    }

    /**
     * Validates that some meta-data and prompt responses conform to this
     * survey.
     *
     * @param metaData
     *        The meta-data to validate.
     *
     * @param promptResponses
     *        The prompt responses to validate.
     *
     * @throws InvalidArgumentException
     *         The meta-data or prompt responses were invalid.
     */
    public void validate(
        final MetaData metaData,
        final Map<String, Object> promptResponses)
        throws InvalidArgumentException {

        // Create an iterator for the survey items.
        Iterator<SurveyItem> surveyItemIter = surveyItems.iterator();

        // Create a map of previously validated responses to use for validating
        // future response's conditions.
        Map<String, Object> checkedResponses = new HashMap<String, Object>();

        // Loop through all of the possible survey items.
        while(surveyItemIter.hasNext()) {
            // Get the survey item.
            SurveyItem surveyItem = surveyItemIter.next();

            // If it's respondable, then check for a response.
            if(surveyItem instanceof Respondable) {
                // Cast the survey item to a prompt.
                Respondable respondable = (Respondable) surveyItem;

                // Get the response.
                Object response =
                    promptResponses.get(surveyItem.getSurveyItemId());

                // Validate the response.
                respondable.validateResponse(response, checkedResponses);

                // Add the response to the list of checked responses.
                checkedResponses.put(surveyItem.getSurveyItemId(), response);
            }
        }

        // Ensure that we used all of the prompt responses.
        Set<String> extraResponses =
            new HashSet<String>(promptResponses.keySet());
        extraResponses.removeAll(checkedResponses.keySet());
        if(! extraResponses.isEmpty()) {
            throw
                new InvalidArgumentException(
                    "More responses exist than prompts in the survey.");
        }
    }

    /**
     * Returns the list of survey items.
     *
     * @return The list of survey items.
     */
    public List<SurveyItem> getSurveyItems() {
        return Collections.unmodifiableList(surveyItems);
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.Schema#getDefinition()
     */
    @Override
    public Concordia getDefinition() {
        // Create the list of prompt definitions.
        List<name.jenkins.paul.john.concordia.schema.Schema> fields =
            new LinkedList<name.jenkins.paul.john.concordia.schema.Schema>();
        for(SurveyItem surveyItem : surveyItems) {
            if(surveyItem instanceof Respondable) {
                // Get the survey item's response schema.
                name.jenkins.paul.john.concordia.schema.Schema schema =
                    ((Respondable) surveyItem).getResponseSchema();

                // If the schema is null, ignore it.
                if(schema == null) {
                    continue;
                }

                // Add the schema to the list of fields.
                fields.add(schema);
            }
        }

        // Build the root schema.
        ObjectSchema rootSchema;
        try {
            rootSchema = new ObjectSchema(
                getDescription(),
                false,
                getId(),
                fields);
        }
        catch(ConcordiaException e) {
            throw new IllegalStateException("The root object was invalid.", e);
        }

        // Build and return the Concordia object.
        try {
            return new Concordia(rootSchema);
        }
        catch(IllegalArgumentException | ConcordiaException e) {
            throw
                new IllegalArgumentException(
                    "The Concordia object could not be built.",
                    e);
        }
    }
}