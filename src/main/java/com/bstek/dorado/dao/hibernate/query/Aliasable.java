package com.bstek.dorado.dao.hibernate.query;



/**
 *@author Kevin.yang
 *@since 2015年6月11日
 */
public interface Aliasable<T> {
 	
	 T as(String alias);


}
