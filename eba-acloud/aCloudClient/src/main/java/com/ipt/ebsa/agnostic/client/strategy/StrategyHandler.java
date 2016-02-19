package com.ipt.ebsa.agnostic.client.strategy;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;


/**
 * Contains logic for resolving create, delete and confirm plans
 * 
 *
 */
public class StrategyHandler {

	private Logger logger = LogManager.getLogger(StrategyHandler.class);


	public enum Action {
		CREATE, CREATE_WAIT, DESTROY_THEN_CREATE, DO_NOTHING, DELETE, UPDATE, UPDATE_WAIT
	}

	/**
	 * Common logic for creates, checked against a strategy. Pass in the
	 * strategy and the result of a lookup, plus some facts for use in
	 * messaging. The response will tell the caller what actions need to be
	 * taken
	 * 
	 * @param strategy
	 * @param entity
	 * @param entityType
	 * @param entityName
	 * @return
	 * @throws StrategyFailureException
	 * @throws InvalidStrategyException
	 */
	public Action resolveCreateStrategy(CmdStrategy strategy, Object entity, String entityType, String entityName, String messageEnd) throws StrategyFailureException, InvalidStrategyException {
		
		if (strategy == null) {
			logger.debug(String.format("No strategy provided for %s '%s' it will be defaulted to 'createOnly'.", entityType, entityName));
			strategy = CmdStrategy.CREATE_ONLY;
		}
		
		boolean exists = false;
		
		if(entity != null && entity instanceof Boolean == false) {
			exists = true;
		} else if (entity != null && entity instanceof Boolean && ((Boolean) entity).booleanValue() == true) {
			exists = true;
		} else {
			exists = false;
		}
		
		Action action = Action.DO_NOTHING;
		switch (strategy) {
		case CREATE_ONLY:
			if (exists) {
				throw new StrategyFailureException(String.format("Strategy %s cannot be completed because %s '%s' already exists%s", strategy, entityType, entityName, messageEnd));
			}
			action = Action.CREATE;
			break;
		case MERGE:
			if (exists) {
				logger.info(String.format("%s '%s' already exists, it will not be recreated (if it is an updateable entity then it will be updated).", entityType, entityName));
				action = Action.UPDATE;
			} else {
				logger.info(String.format("%s '%s' does not exist, it will be created.", entityType, entityName));
				action = Action.CREATE;
			}
			break;
		case CREATE_ONLY_WAIT:
			if (exists) {
				throw new StrategyFailureException(String.format("Strategy %s cannot be completed because %s '%s' already exists%s", strategy, entityType, entityName, messageEnd));
			}
			action = Action.CREATE_WAIT;
			break;
		case MERGE_WAIT:
			if (exists) {
				logger.info(String.format("%s '%s' already exists, it will not be recreated (if it is an updateable entity then it will be updated).", entityType, entityName));
				action = Action.UPDATE_WAIT;
			} else {
				logger.info(String.format("%s '%s' does not exist, it will be created.", entityType, entityName));
				action = Action.CREATE_WAIT;
			}
			break;
		case OVERWRITE:
			if (exists) {
				logger.info(String.format("%s '%s' already exists, it will need to be destroyed and then recreated.", entityType, entityName));
				action = Action.DESTROY_THEN_CREATE;
			} else {
				logger.info(String.format("%s '%s' does not exist, it will be created.", entityType, entityName));
				action = Action.CREATE;
			}
			
			break;
		default:
			throw new InvalidStrategyException(String.format("Strategy '%s' is not valid when creating.", strategy));
		}
		return action;
	}

	/**
	 * Common logic for deletes, resolved against a strategy and the result of a
	 * lookup, plus some facts for use in messaging. The response will tell the
	 * caller what actions need to be taken
	 * 
	 * @param strategy
	 * @param entity
	 * @param entityType
	 * @param entityName
	 * @return
	 * @throws StrategyFailureException
	 * @throws InvalidStrategyException
	 */
	public Action resolveDeleteStrategy(Object entity, String entityType, String entityName, String messageEnd) throws StrategyFailureException, InvalidStrategyException {
			
		Action action = Action.DO_NOTHING;
		if (entity != null && !Boolean.FALSE.equals(entity)) {
			logger.info(String.format("%s '%s' exists, it will deleted %s", entityType, entityName, messageEnd));
			action = Action.DELETE;
		} else {
			logger.info(String.format("%s '%s' does not exist, nothing to do.", entityType, entityName));
			action = Action.DO_NOTHING;
		}
		return action;
	}

	/**
	 * Common logic for confirms, resolved against a strategy and the result of
	 * a lookup, plus some facts for use in messaging. The response is always
	 * DO-NOTHING unless there is s strategy failure in which case an exception
	 * is thrown.
	 * 
	 * @param strategy
	 * @param entity
	 * @param entityType
	 * @param entityName
	 * @return
	 * @throws StrategyFailureException
	 * @throws InvalidStrategyException
	 */
	public Action resolveConfirmStrategy(CmdStrategy strategy, Object entity, String entityType, String entityName, String messageEnd) throws StrategyFailureException, InvalidStrategyException {
		if (strategy == null) {
			throw new StrategyFailureException(String.format("Strategy has not been specified.  Do not know what to confirm about %s '%s'.", strategy,
					entityType, entityName));
		}
		
		boolean entityExists = entity != null && !Boolean.FALSE.equals(entity);
		
		switch (strategy) {
		case EXISTS:
			if (entityExists) {
				logger.info(String.format("%s '%s' exists %s", entityType, entityName, messageEnd));
			} else {
				throw new StrategyFailureException(String.format("Strategy %s cannot be completed.  Expected %s '%s' to exist, but it does not.", strategy,
						entityType, entityName));
			}
			break;
		case DOESNOTEXIST:
			if (entityExists) {
				throw new StrategyFailureException(String.format("Strategy %s cannot be complete. Expected %s '%s' not to exist, but it does", strategy, entityType,
						entityName));
			} else {
				logger.info(String.format("%s '%s' does not exist %s", entityType, entityName, messageEnd));
			}
			break;
		default:
			throw new InvalidStrategyException("Strategy '" + strategy + "' is not valid when confirming.");
		}
		return Action.DO_NOTHING;
	}

}
