package com.bstek.dorado.dao.hibernate;

import java.util.List;


/**
 *@author Kevin.yang
 *@since 2015年5月16日
 */
public class MappingResourcesRegister {
	private String sessionFactoryName;
	private List<String> mappingResources;
	
	public String getSessionFactoryName() {
		return sessionFactoryName;
	}
	public void setSessionFactoryName(String sessionFactoryName) {
		this.sessionFactoryName = sessionFactoryName;
	}
	public List<String> getMappingResources() {
		return mappingResources;
	}
	public void setMappingResources(List<String> resources) {
		this.mappingResources = resources;
	}
	
	
}
