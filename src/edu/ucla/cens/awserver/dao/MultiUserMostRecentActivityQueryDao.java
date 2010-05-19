package edu.ucla.cens.awserver.dao;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.domain.MostRecentActivityQueryResult;
import edu.ucla.cens.awserver.domain.SimpleUser;
import edu.ucla.cens.awserver.request.AwRequest;

/**
 * DAO for looking up the most recent prompt response and mobility uploads.
 * 
 * @author selsky
 */
public class MultiUserMostRecentActivityQueryDao extends SingleUserMostRecentActivityQueryDao {
	private static Logger _logger = Logger.getLogger(MultiUserMostRecentActivityQueryDao.class);
	private Dao _findAllUsersForCampaignDao;
	
	/**
	 * 
	 */
	public MultiUserMostRecentActivityQueryDao(DataSource dataSource, Dao findAllUsersForCampaignDao) {
		super(dataSource);
		
		if(null == findAllUsersForCampaignDao) {
			throw new IllegalArgumentException("a non-null Dao is required");
		}
		
		_findAllUsersForCampaignDao = findAllUsersForCampaignDao;
	}
	
	@Override
	public void execute(AwRequest awRequest) {
		_findAllUsersForCampaignDao.execute(awRequest);
		
		List<?> userList = awRequest.getResultList();
		List<MostRecentActivityQueryResult> results = new ArrayList<MostRecentActivityQueryResult>();
		
		int size = userList.size();
		
 		if(_logger.isDebugEnabled()) {
 			_logger.debug("about to run queries for " + size + " users");
 		}
		
		for(int i = 0; i < size; i++) {
			
			SimpleUser su = (SimpleUser) userList.get(i);
			MostRecentActivityQueryResult result = executeSqlForSingleUser(Integer.parseInt(awRequest.getUser().getCurrentCampaignId()), su.getId(), su.getUserName());
			results.add(result);
		}
		
		awRequest.setResultList(results);
	}
}