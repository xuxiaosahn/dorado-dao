package com.bstek.dorado.dao.hibernate.query;

import org.hibernate.Query;

/**
 *@author Kevin.yang
 *@since 2015年6月12日
 */
public interface QueryCallback<T extends Query> {
	void initQuery(T query);
}
