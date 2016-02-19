package com.ipt.ebsa.agnostic.client.aws.manager;

import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.ipt.ebsa.agnostic.client.exception.FatalException;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;
import com.jcabi.aspects.Loggable;
import com.jcabi.aspects.RetryOnFailure;
import com.jcabi.aspects.Timeable;

/**
 * Manages retrying and waiting
 * 
 *
 */
public class AwsRetryManager {

	/** Logger */
	// private static final Logger logger =
	// Logger.getLogger(AwsRetryManager.class);

	/**
	 * Defines a block of code that can be retried
	 *
	 * @param <T>
	 */
	public interface Retryable<T> {
		public T run();
	}

	/**
	 * Make up to 60 attempts to execute the given Retryable, retrying every 5
	 * seconds if it throws an AmazonServiceException
	 * 
	 * @param retryable
	 * @return
	 */
	@RetryOnFailure(attempts = 60, delay = 5, unit = TimeUnit.SECONDS, types = { AmazonServiceException.class, AmazonClientException.class }, ignore = { NoSuchEntityException.class }, randomize = false)
	public static <T> T run(Retryable<T> retryable) {
		return retryable.run();
	}

	/**
	 * Make up to 60 attempts to execute the given Retryable, retrying every 5
	 * seconds if it throws an AmazonServiceException
	 * 
	 * @param retryable
	 * @return
	 * @throws FatalException 
	 */
	@RetryOnFailure(attempts = 60, delay = 5, unit = TimeUnit.SECONDS, types = { AmazonServiceException.class, AmazonClientException.class }, randomize = false)
	public static <T> T run2(Retryable<T> retryable) throws FatalException {
		T retVal = null;
		try {
			retVal =  retryable.run();
		} catch (AmazonServiceException e) {
			if(AwsExceptionManager.isFatal(e)) {
				throw new FatalException(e.getMessage());
			} else {
				throw e;
			}
		}
		return retVal;
	}

	/**
	 * Defines a condition which can be wait on until it evaluates to true
	 *
	 */
	public interface WaitCondition {
		public boolean evaluate();
	}

	/**
	 * Wait for up to 2 minutes for the given condition to be true, checking
	 * every sleepMs milliseconds. If the condition is not met after 6 minutes,
	 * an UnsafeOperationException with the given errorMessage is thrown. If
	 * extendedWait is true, the wait is extended from 2 minutes to 1 hour.
	 * 
	 * @param condition
	 * @param errorMessage
	 * @param sleepMs
	 * @param extendedWait
	 * @throws UnSafeOperationException
	 */
	public static void waitFor(WaitCondition condition, String errorMessage, int sleepMs, boolean extendedWait) throws UnSafeOperationException {
		int repeats = extendedWait ? 30 : 3;
		for (int i = 0; i < repeats; i++) {
			if (waitFor(condition, sleepMs)) {
				return;
			}
		}
		throw new UnSafeOperationException(errorMessage);
	}

	/**
	 * Wait for the given condition to be true, checking every sleepMs
	 * milliseconds. Throw no error on timeout expire.
	 * 
	 * @param condition
	 * @param errorMessage
	 * @param sleepMs
	 * @param extendedWait
	 * @throws UnSafeOperationException
	 */
	public static void waitFor(WaitCondition condition, int sleepMs, int repeats) throws UnSafeOperationException {
		for (int i = 0; i < repeats; i++) {
			if (waitFor(condition, sleepMs)) {
				return;
			}
		}
	}

	/**
	 * Wait for the given condition to be true, checking every sleepMs milliseconds. Throw no error on timeout expire. 
	 * @param condition
	 * @param errorMessage
	 * @param sleepMs
	 * @param extendedWait
	 * @throws UnSafeOperationException
	 */
	public static void waitForBootstrap(WaitCondition condition, int sleepMs, int repeats) throws UnSafeOperationException {
		for (int i = 0; i < repeats; i++) {
			if (waitForBootstrap(condition, sleepMs)) {
				return;
			}
		}
	}
	
	/**
	 * Returns true if the given condition is true within 2 minutes, checking every sleepMs milliseconds
	 * @param condition
	 * @param sleepMs
	 * @return
	 */
	@Timeable(limit = 5, unit = TimeUnit.MINUTES)
	private static boolean waitFor(WaitCondition condition, int sleepMs) {
		boolean interrupted = false;
		try {
			while (!condition.evaluate()) {
				interrupted = Thread.interrupted();
				Thread.sleep(sleepMs);
				interrupted = Thread.interrupted();
			}
		} catch (InterruptedException e) {
			interrupted = true;
		}
		return !interrupted;
	}
	
	/**
	 * Returns true if the given condition is true within 2 minutes, checking every sleepMs milliseconds
	 * @param condition
	 * @param sleepMs
	 * @return
	 */
	@Timeable(limit=30, unit=TimeUnit.MINUTES)
	private static boolean waitForBootstrap(WaitCondition condition, int sleepMs) {
		boolean interrupted = false;
		try {
			while (!condition.evaluate()) {
				interrupted = Thread.interrupted();
				Thread.sleep(sleepMs);
				interrupted = Thread.interrupted();
			}
		} catch (InterruptedException e) {
			interrupted = true;
		}
		return !interrupted;
	}
}
