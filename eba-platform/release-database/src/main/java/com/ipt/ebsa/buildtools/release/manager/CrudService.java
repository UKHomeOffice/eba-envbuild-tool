package com.ipt.ebsa.buildtools.release.manager;

import java.util.List;
import java.util.Map;

/**
 * Taken from http://www.adam-bien.com/roller/abien/entry/generic_crud_service_aka_dao
 * @author scowx
 *
 */
public interface CrudService<T> {
    public T create(T t);
    public T find(Class<T> type, Object id);
    public T update(T t);
    public void delete(Class<T> type, Object id);
    public List<T> findWithNamedQuery(Class<T> type, String queryName);
    public List<T> findWithNamedQuery(Class<T> type, String queryName, int resultLimit);
    public List<T> findWithNamedQuery(Class<T> type, String namedQueryName, Map<String,Object> parameters);
    public List<T> findWithNamedQuery(Class<T> type, String namedQueryName, Map<String,Object> parameters, int resultLimit);
    public T findOnlyResultWithNamedQuery(Class<T> type, String queryName);
    public T findOnlyResultWithNamedQuery(Class<T> type, String namedQueryName, Map<String,Object> parameters);
}