package com.ipt.ebsa.database.manager;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParamFactory {
    
    private Map<String,Object> parameters = null;
    
    private ParamFactory(String name,Object value){
        this.parameters = new LinkedHashMap<String, Object>();
        this.parameters.put(name, value);
    }
    public static ParamFactory with(String name,Object value){
        return new ParamFactory(name, value);
    }
    public ParamFactory and(String name,Object value){
        this.parameters.put(name, value);
        return this;
    }
    public Map<String, Object> parameters(){
        return this.parameters;
    }
}