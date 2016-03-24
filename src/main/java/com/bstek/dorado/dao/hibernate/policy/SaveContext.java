package com.bstek.dorado.dao.hibernate.policy;

import org.hibernate.Session;

/**
 *@author Kevin.yang
 *@since 2015年9月9日
 */
public class SaveContext {
	private Object entity;
	private Session session;
	private Object parent;
	
	@SuppressWarnings("unchecked")
	public <T> T getEntity() {
		return (T) entity;
	}
	public void setEntity(Object entity) {
		this.entity = entity;
	}
	public Session getSession() {
		return session;
	}
	public void setSession(Session session) {
		this.session = session;
	}
	@SuppressWarnings("unchecked")
	public <T> T getParent() {
		return (T) parent;
	}
	public void setParent(Object parent) {
		this.parent = parent;
	}
	
	
	
}
