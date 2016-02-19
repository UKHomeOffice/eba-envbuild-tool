package com.ipt.ebsa.buildtools.release.util;

/**
 * Stuff we shouldn't have to do, but do for one reason or another.
 * 
 * @author David Manning
 */
public class ClassLoaderUtils {

	/**
	 * *** ONLY use this method if completely necessary!***
	 * 
	 * Switches the ClassLoaded to that of the 'context' before doing the supplied work.
	 * 
	 * Ooh this is nasty.
	 * The version of hibernates antlr library conflicts with the version of the antlr library that jenkins uses.
	 * It only happens when I use the "IN" syntax in queries which is what happens when I fetch the component versions (below)
	 * The classloader shenanigans below are there to make sure that it is the antlr library that I supplied 
	 * that is used and not the one that jenkins supplies. 
	 * I would really like to know how jenkins does its classloader instantiation.  Ideally it would be the plugins classes
	 * which are loaded first.  This will have to do for now....technical debt.
	 * 
	 * Here is the problem manifested in weblogic, unfortunately these workarounds do not work for us.
	 * http://stackoverflow.com/questions/2702266/classnotfoundexception-hqltoken-when-running-in-weblogic
	 * 
	 * Here is the stack trace of the error we get:
	 * 
	 *  java.lang.IllegalArgumentException: org.hibernate.QueryException: ClassNotFoundException: org.hibernate.hql.ast.HqlToken [select x from com.ipt.ebsa.buildtools.release.entities.ComponentVersion x where x.id in (:componentIds0_, :componentIds1_, :componentIds2_) order by x.group asc, x.artifact asc, x.version desc]
	 *  	at org.hibernate.ejb.AbstractEntityManagerImpl.convert(AbstractEntityManagerImpl.java:1376)
	 *  	at org.hibernate.ejb.AbstractEntityManagerImpl.convert(AbstractEntityManagerImpl.java:1317)
	 *  	at org.hibernate.ejb.QueryImpl.getResultList(QueryImpl.java:255)
	 *  	at com.ipt.ebsa.buildtools.release.manager.CrudServiceImpl.findWithNamedQuery(CrudServiceImpl.java:97)
	 * @throws Exception 
	 *  	
	 */
	public static <T> T doInAppContext(Object context, TypedRunnable<T> runnable) throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
		    if ( cl != null ) {
		     	Thread.currentThread().setContextClassLoader(context.getClass().getClassLoader());			
	    	}
		    
		    return runnable.run();
		} catch (Exception e) {			
			e.printStackTrace();
			throw new Exception("Error switching class loader context or running item of work in new context", e);
		} finally {
			if ( cl != null ) {
				//put things back the way they were
				Thread.currentThread().setContextClassLoader(cl);			
			}
		}
	}
	
	public interface TypedRunnable<T> {
		public T run() throws Exception;
	}
}
