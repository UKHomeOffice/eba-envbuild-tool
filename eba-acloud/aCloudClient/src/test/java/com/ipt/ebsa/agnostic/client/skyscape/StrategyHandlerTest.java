package com.ipt.ebsa.agnostic.client.skyscape;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.code.tempusfugit.concurrency.ConcurrentTestRunner;
import com.ipt.ebsa.agnostic.client.skyscape.exception.InvalidStrategyException;
import com.ipt.ebsa.agnostic.client.skyscape.exception.StrategyFailureException;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler;
import com.ipt.ebsa.agnostic.client.strategy.StrategyHandler.Action;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.jcabi.aspects.Loggable;

/**
 * Tests core logic encapsulated in the CloudManager
 * 
 *
 */
@Loggable(prepend = true)
@RunWith(ConcurrentTestRunner.class)
public class StrategyHandlerTest extends VmWareBaseTest {

	private static final String MSG_END = "in the container thingy";
	private static final String NAME = "Cumulo nimbus";
	private static final String TYPE = "Cloud thing";
	private static final String exists = "exist";
	private static final String doesNotExist = null;
	private static final StrategyHandler strategyHanlder = new StrategyHandler();
	
	@BeforeClass
	public static void setUpBeforeVmWareBaseTestClass() throws InterruptedException {
		//override setup as its not needed and makes concurrent testing possible
	}

	@Test
	public void testResolveCreateStrategy() throws StrategyFailureException, InvalidStrategyException {
		/* Negative tests (Errors are expected) */

		try {
			strategyHanlder.resolveCreateStrategy(CmdStrategy.EXISTS, doesNotExist, TYPE, NAME, MSG_END);
		} catch (InvalidStrategyException e) {
		}
		try {
			strategyHanlder.resolveCreateStrategy(CmdStrategy.DOESNOTEXIST, exists, TYPE, NAME, MSG_END);
		} catch (InvalidStrategyException e) {
		}

		try {
			strategyHanlder.resolveCreateStrategy(CmdStrategy.CREATE_ONLY, exists, TYPE, NAME, MSG_END);
		} catch (StrategyFailureException e) {
		}

		/* Positive test */
		Assert.assertEquals(Action.DESTROY_THEN_CREATE, strategyHanlder.resolveCreateStrategy(CmdStrategy.OVERWRITE, exists, TYPE, NAME, MSG_END));
		Assert.assertEquals(Action.CREATE, strategyHanlder.resolveCreateStrategy(CmdStrategy.OVERWRITE, doesNotExist, TYPE, NAME, MSG_END));
		Assert.assertEquals(Action.UPDATE, strategyHanlder.resolveCreateStrategy(CmdStrategy.MERGE, exists, TYPE, NAME, MSG_END));
		Assert.assertEquals(Action.CREATE, strategyHanlder.resolveCreateStrategy(CmdStrategy.MERGE, doesNotExist, TYPE, NAME, MSG_END));
		Assert.assertEquals(Action.CREATE, strategyHanlder.resolveCreateStrategy(CmdStrategy.CREATE_ONLY, doesNotExist, TYPE, NAME, MSG_END));

	}

	@Test
	public void testDeleteCreateStrategy() throws StrategyFailureException, InvalidStrategyException {

		/* Positive test */
		Assert.assertEquals(Action.DELETE, strategyHanlder.resolveDeleteStrategy(exists, TYPE, NAME, MSG_END));
		Assert.assertEquals(Action.DO_NOTHING, strategyHanlder.resolveDeleteStrategy(doesNotExist, TYPE, NAME, MSG_END));

	}

	@Test
	public void testConfirmCreateStrategy() throws StrategyFailureException, InvalidStrategyException {

		/* Negative tests (Errors are expected) */
		try {
			strategyHanlder.resolveConfirmStrategy(CmdStrategy.CREATE_ONLY, exists, TYPE, NAME, MSG_END);
		} catch (InvalidStrategyException e) {
		}

		try {
			strategyHanlder.resolveConfirmStrategy(CmdStrategy.MERGE, exists, TYPE, NAME, MSG_END);
		} catch (InvalidStrategyException e) {
		}
		try {
			strategyHanlder.resolveConfirmStrategy(CmdStrategy.OVERWRITE, exists, TYPE, NAME, MSG_END);
		} catch (InvalidStrategyException e) {
		}
		try {
			strategyHanlder.resolveConfirmStrategy(CmdStrategy.EXISTS, doesNotExist, TYPE, NAME, MSG_END);
		} catch (StrategyFailureException e) {
		}
		try {
			strategyHanlder.resolveConfirmStrategy(CmdStrategy.DOESNOTEXIST, exists, TYPE, NAME, MSG_END);
		} catch (StrategyFailureException e) {
		}

		/* Positive tests */
		Assert.assertEquals(Action.DO_NOTHING, strategyHanlder.resolveConfirmStrategy(CmdStrategy.EXISTS, exists, TYPE, NAME, MSG_END));
		Assert.assertEquals(Action.DO_NOTHING, strategyHanlder.resolveConfirmStrategy(CmdStrategy.DOESNOTEXIST, doesNotExist, TYPE, NAME, MSG_END));

	}

}
