package com.ipt.ebsa.agnostic.client.aws.manager;

import org.apache.commons.lang.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.AmazonServiceException;
import com.google.code.tempusfugit.concurrency.ConcurrentTestRunner;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.Retryable;
import com.ipt.ebsa.agnostic.client.aws.manager.AwsRetryManager.WaitCondition;
import com.ipt.ebsa.agnostic.client.exception.UnSafeOperationException;

/**
 * JUnit tests for the AwsRetryManager
 *
 */
@RunWith(ConcurrentTestRunner.class)
public class AwsRetryManagerTest {
	
	@Test(timeout=30000)
	public void test5Retrys() throws Exception {
		Retryable<Integer> retryable = new Retryable<Integer>() {
			private Integer runCount = 0;
			@Override
			public Integer run() {
				++runCount;
				if (runCount < 5) {
					throw new AmazonServiceException("");
				}
				return runCount;
			}
		};
		
		Integer runCount = AwsRetryManager.run(retryable);
		Assert.assertEquals(5, runCount.intValue());
	}
	
	@Test(expected=AmazonServiceException.class, timeout=130000)
	public void testRetrysExceeded() throws Exception {
		Retryable<Integer> retryable = new Retryable<Integer>() {
			private Integer runCount = 0;
			@Override
			public Integer run() {
				++runCount;
				throw new AmazonServiceException("");
			}
		};
		
		try {
			AwsRetryManager.run(retryable);
		} catch (AmazonServiceException e) {
			Assert.assertEquals(10, FieldUtils.readField(retryable, "runCount", true));
			throw e;
		}
		Assert.fail("Expected AmazonServiceException because number of retries was exceeded");
	}

	@Test(timeout=10000)
	public void testWait() throws Exception {
		
		WaitCondition condition = new WaitCondition() {
			long waitUntil = System.currentTimeMillis() + 5000;
			@Override
			public boolean evaluate() {
				return System.currentTimeMillis() >= waitUntil;
			}
		};
		
		AwsRetryManager.waitFor(condition, "", 500, false);
	}
	
	@Test(expected=UnSafeOperationException.class, timeout=180000)
	public void testWaitExpires() throws Exception {
		
		WaitCondition condition = new WaitCondition() {
			@Override
			public boolean evaluate() {
				return false;
			}
		};
		
		AwsRetryManager.waitFor(condition, "", 5000, false);
		Assert.fail("Expected UnSafeOperationException due to wait timeout");
	}
}
