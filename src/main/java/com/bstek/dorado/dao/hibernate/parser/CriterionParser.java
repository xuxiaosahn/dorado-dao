package com.bstek.dorado.dao.hibernate.parser;

import com.bstek.dorado.data.provider.filter.SingleValueFilterCriterion;


/**
 *@author Kevin.yang
 *@since 2015年8月16日
 */
public interface CriterionParser<T> {
	T parse(SingleValueFilterCriterion criterion);
}
