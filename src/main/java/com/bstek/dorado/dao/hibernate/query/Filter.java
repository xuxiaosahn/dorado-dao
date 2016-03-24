package com.bstek.dorado.dao.hibernate.query;

import com.bstek.dorado.dao.hibernate.policy.LinqContext;

/**
 *@author Kevin.yang
 *@since 2015年6月11日
 */
public interface Filter {
	boolean invoke(LinqContext linqContext);
}
