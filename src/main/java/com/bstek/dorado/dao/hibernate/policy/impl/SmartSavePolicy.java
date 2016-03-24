package com.bstek.dorado.dao.hibernate.policy.impl;

import org.hibernate.Session;

import com.bstek.dorado.dao.hibernate.policy.SaveContext;
import com.bstek.dorado.dao.hibernate.policy.SavePolicy;
import com.bstek.dorado.data.entity.EntityState;
import com.bstek.dorado.data.entity.EntityUtils;


/**
 *@author Kevin.yang
 *@since 2015年5月17日
 */
public class SmartSavePolicy implements SavePolicy {

	@Override
	public void apply(SaveContext context) {
		Object entity = context.getEntity();
		Session session = context.getSession();
		EntityState state = EntityUtils.getState(entity);
		if (EntityState.NEW.equals(state)) {
			session.save(entity);
		} else if (EntityState.MODIFIED.equals(state) 
				|| EntityState.MOVED.equals(state)) {
			session.update(entity);
		} else if (EntityState.DELETED.equals(state)) {
			session.delete(entity);
		}
	}

}
