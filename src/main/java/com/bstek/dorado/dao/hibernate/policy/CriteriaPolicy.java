package com.bstek.dorado.dao.hibernate.policy;



/**
 *@author Kevin.yang
 *@since 2015年5月20日
 */
public interface CriteriaPolicy<T> {
	T apply(CriteriaContext context);
}
