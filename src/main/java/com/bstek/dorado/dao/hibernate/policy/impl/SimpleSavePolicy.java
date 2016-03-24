package com.bstek.dorado.dao.hibernate.policy.impl;

import com.bstek.dorado.dao.hibernate.policy.SaveContext;
import com.bstek.dorado.dao.hibernate.policy.SavePolicy;


/**
 *@author Kevin.yang
 *@since 2015年5月17日
 */
public class SimpleSavePolicy implements SavePolicy {

	@Override
	public void apply(SaveContext context) {
		if (context.getParent() == null) {
			context.getSession().save(context.getEntity());
		}
	}

}
