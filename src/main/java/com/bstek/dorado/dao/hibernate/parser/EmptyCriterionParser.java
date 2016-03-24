package com.bstek.dorado.dao.hibernate.parser;

import com.bstek.dorado.data.provider.filter.SingleValueFilterCriterion;

/**
 *@author Kevin.yang
 * @param <V>
 *@since 2015年8月16日
 */
public class EmptyCriterionParser<T> implements CriterionParser<T> {

	@Override
	public T parse(SingleValueFilterCriterion criterion) {
		return null;
	}

}
