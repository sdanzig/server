package edu.ucla.cens.awserver.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Cache for the preferences in the database.
 * 
 * @author John Jenkins
 */
public class PreferenceCache extends KeyValueCache {
	private static Logger _logger = Logger.getLogger(PreferenceCache.class);
	
	private static final String SQL_GET_CAMPAIGN_PRIVACY_STATES_AND_IDS = "SELECT p_key, p_value " +
																		  "FROM preference";
	
	// When we are requesting a cache in the Spring files, we use this
	// to reference which key we want.
	public static final String CACHE_KEY = "preferenceCache";
	
	// Known campaign privacy states.
	public static final String KEY_DEFAULT_CAN_CREATE_PRIVILIEGE = "default_can_create_privilege";
	public static final String KEY_DEFAULT_SURVEY_RESPONSE_SHARING_STATE = "default_survey_response_sharing_state";
	
	protected static Map<String, String> _keyValueMap = new HashMap<String, String>();
	
	// The last time we refreshed our cache in milliseconds since epoch.
	protected static long _lastUpdateTimestamp = -1;
	// The number of milliseconds between refreshes of the local cache.
	protected static long _updateFrequency = -1;
	
	/**
	 * Default constructor set private to make this a Singleton.
	 */
	private PreferenceCache() {
		// Does nothing.
	}
	
	/**
	 * Sets the initial update frequency for this object. This is only called
	 * by Spring as it is a non-static call and this object can never be
	 * instantiated in code. 
	 * 
	 * @complexity O(1)
	 * 
	 * @param frequencyInMilliseconds The frequency that updates should be
	 * 								  checked for in milliseconds. The system
	 * 								  will still only do updates when a 
	 * 								  request is being made and the cache has
	 * 								  expired to prevent unnecessary, 
	 * 								  premature checks to the database.
	 */
	public synchronized void setUpdateFrequency(long frequencyInMilliseconds) {
		if(frequencyInMilliseconds < 1000) {
			throw new IllegalArgumentException("The update frequency must be a positive integer greater than or equal to 1000 milliseconds.");
		}
		
		_updateFrequency = frequencyInMilliseconds;
	}
	
	/**
	 * Compares the current timestamp with the last time we did an update plus
	 * the amount of time between updates. If our cache has become stale, we
	 * attempt to update it and, if successful, we update the time of the last
	 * update.
	 * 
	 * Then, we check to see if such the key exists in our cache. If not, we
	 * throw an exception because, if someone is querying for a key that 
	 * doesn't exist, we need to bring it to their immediate attention rather
	 * than return an incorrect value. Otherwise, the value is returned.
	 * 
	 * It is recommended but not required to use the PRIVACY_STATE_* constants
	 * defined in this class when possible.
	 * 
	 * @complexity O(n) if a refresh is required; otherwise, O(1) assuming the
	 * 			   map can lookup at that complexity on the average case.
	 * 
	 * @param state The key to use to lookup the value.
	 * 
	 * @return The value stored with the parameterized key.
	 * 
	 * @throws CacheMissException Thrown if no such state exists.
	 */
	public static String lookup(String key) throws CacheMissException {		
		// If the lookup table is out-of-date, refresh it.
		if((_lastUpdateTimestamp + _updateFrequency) <= System.currentTimeMillis()) {
			refreshMap();
		}
		
		// If the key exists in the lookup table, return its value.
		if(_keyValueMap.containsKey(key)) {
			return _keyValueMap.get(key);
		}
		// Otherwise, throw an exception that it is an unknown state.
		else {
			throw new CacheMissException("Unknown key: " + key);
		}
	}
	
	/**
	 * Returns all the known keys.
	 * 
	 * @return All known keys.
	 */
	public static Set<String> getStates() {
		// If the lookup table is out-of-date, refresh it.
		if((_lastUpdateTimestamp + _updateFrequency) <= System.currentTimeMillis()) {
			refreshMap();
		}
		
		return _keyValueMap.keySet();
	}
	
	/**
	 * Reads the database for the information in the lookup table and
	 * populates its map with the gathered information. If there is an issue
	 * reading the database, it will just remain with the current lookup table
	 * it has.
	 * 
	 * @complexity O(n) where n is the number of states in the database.
	 */
	private static synchronized void refreshMap() {
		// Only one thread should be updating this information at a time. Once
		// other threads enter, they should check to see if an update was just
		// done and, if so, should abort a second update.
		if((_lastUpdateTimestamp + _updateFrequency) > System.currentTimeMillis()) {
			return;
		}
		
		// This is the JdbcTemplate we will use for our query.
		JdbcTemplate jdbcTemplate = new JdbcTemplate(_dataSource);
		
		// Get all the keys and their corresponding values.
		List<?> keyAndValue;
		try {
			keyAndValue = jdbcTemplate.query(SQL_GET_CAMPAIGN_PRIVACY_STATES_AND_IDS,
											new RowMapper() {
												@Override
												public Object mapRow(ResultSet rs, int row) throws SQLException {
													return new KeyAndValue(rs.getString("p_key"), rs.getString("p_value"));
												}
											});
		}
		catch(org.springframework.dao.DataAccessException e) {
			_logger.error("Error executing SQL '" + SQL_GET_CAMPAIGN_PRIVACY_STATES_AND_IDS + "'. Aborting cache refresh.");
			return;
		}
		
		// Clear the list and begin populating it with the new information.
		_keyValueMap.clear();
		ListIterator<?> keyAndValueIter = keyAndValue.listIterator();
		while(keyAndValueIter.hasNext()) {
			KeyAndValue currStateAndId = (KeyAndValue) keyAndValueIter.next();
			_keyValueMap.put(currStateAndId._key, currStateAndId._value);
		}
		
		_lastUpdateTimestamp = System.currentTimeMillis();
	}
}
