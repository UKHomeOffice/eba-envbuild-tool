package com.ipt.ebsa.buildtools.release.manager;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

public class CrudServiceImpl<T> implements CrudService<T> {
	   
    private EntityManager em;
    
    public CrudServiceImpl(EntityManager em) {
    	this.em = em;
    }
        
    public  T create(T t) {
        this.em.persist(t);
        this.em.flush();
        this.em.refresh(t);
        return t;
    }

    public  T find(Class<T> type, Object id) {
       return this.em.find(type, id);
    }

    public void delete(Class<T> type,Object id) {
       Object ref = this.em.getReference(type, id);
       this.em.remove(ref);
    }

    public  T update(T t) {
        return this.em.merge(t);
    }

    public T findOnlyResultWithNamedQuery(Class<T> type, String namedQueryName){
    	List<T> list = this.em.createNamedQuery(namedQueryName, type).setMaxResults(2).getResultList();
    	if (list != null && list.size() != 0) {
    		if (list.size() > 1) {
    			throw new RuntimeException("Expected one result from "+namedQueryName+", got more");
    		}
    		else {
    			return list.get(0);
    		}
    	}
    	else {
    		return null;
    	}
    }
    
    public T findOnlyResultWithNamedQuery(Class<T> type, String namedQueryName, Map<String, Object> parameters){
    	List<T> list = findWithNamedQuery(type, namedQueryName, parameters, 2);
        if (list != null && list.size() != 0) {
    		if (list.size() > 1) {
    			throw new RuntimeException("Expected one result from "+namedQueryName+", got more");
    		}
    		else {
    			return list.get(0);
    		}
    	}
    	else {
    		return null;
    	}
    }
    
    public List<T> findWithNamedQuery(Class<T> type, String namedQueryName){
        return this.em.createNamedQuery(namedQueryName, type).getResultList();
    }
    
    public List<T> findWithNamedQuery(Class<T> type, String namedQueryName, Map<String, Object> parameters){
        return findWithNamedQuery(type, namedQueryName, parameters, 0);
    }

    public List<T> findWithNamedQuery(Class<T> type, String queryName, int resultLimit) {
        return this.em.createNamedQuery(queryName, type).setMaxResults(resultLimit).getResultList();    
    }

    @SuppressWarnings("unchecked")
    public List<T> findByNativeQuery(String sql) {
        return (List<T>) this.em.createNativeQuery(sql).getResultList();
    }
    
    public List<T> findWithNamedQuery(Class<T> type, String namedQueryName, Map<String,Object> parameters,int resultLimit){
        Set<Entry<String,Object>> rawParameters = parameters.entrySet();
        TypedQuery<T> query = this.em.createNamedQuery(namedQueryName, type);
        if(resultLimit > 0)
            query.setMaxResults(resultLimit);
        for (Entry<String,Object> entry : rawParameters) {
            query.setParameter(entry.getKey().toString(), entry.getValue());
        }
        return (List<T>) query.getResultList();
    }
}