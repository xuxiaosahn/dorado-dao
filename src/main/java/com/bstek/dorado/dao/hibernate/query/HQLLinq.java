package com.bstek.dorado.dao.hibernate.query;

import java.util.List;

import org.hibernate.Query;

import com.bstek.dorado.dao.hibernate.parser.CriterionParser;
import com.bstek.dorado.data.provider.Criteria;
import com.bstek.dorado.data.provider.Page;

/**
 *@author Kevin.yang
 *@since 2015年6月12日
 */
public interface HQLLinq extends Aliasable<HQLLinq> {
	HQLLinq select();
	   
	HQLLinq select(String... properties);

	HQLLinq from(String... fromParts);

	HQLLinq from(Class<?> entityClass);
	
	HQLLinq where(Criteria criteria);
	
	HQLLinq where(Criteria criteria, String alias);
	
	HQLLinq where(Criteria criteria, String... criterions);
	
	HQLLinq where(Criteria criteria, String alias,
			String... criterions);
	
	HQLLinq add(String... criterions);

	HQLLinq orders(String... orders);

	HQLLinq setPage(Page<?> page);

	HQLLinq setSessionFactoryName(String sessionFactoryName);
	
	HQLLinq toEntity();

	HQLLinq filter(Filter filter);
	
	Page<?> paging(Page<?> page);

	Page<?> paging();
 
	<T> List<T> list();
 
	<T> T uniqueResult();
 
	int executeUpdate();
 
	HQLLinq setEntityClass(Class<?> entityClass);
	
	Page<?> paging(QueryCallback<Query> queryCallback);

	<T> List<T> list(QueryCallback<Query> queryCallback);
	 
	<T> T uniqueResult(QueryCallback<Query> queryCallback);
	 
	int executeUpdate(QueryCallback<Query> queryCallback);
	 
	Aliasable<HQLLinq> join(String... tables);
	
	HQLLinq setParameter(String name, Object value);
	
	HQLLinq addIfNotNull(Object target, String... criterions);

	HQLLinq addIf(boolean need, String... criterions);

	HQLLinq addParser(CriterionParser<StringBuilder> criterionParser);

	
}
