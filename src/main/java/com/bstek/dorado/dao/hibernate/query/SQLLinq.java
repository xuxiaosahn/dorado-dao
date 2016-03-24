package com.bstek.dorado.dao.hibernate.query;

import java.util.List;

import org.hibernate.SQLQuery;

import com.bstek.dorado.dao.hibernate.parser.CriterionParser;
import com.bstek.dorado.data.provider.Criteria;
import com.bstek.dorado.data.provider.Page;

/**
 *@author Kevin.yang
 *@since 2015年6月12日
 */
public interface SQLLinq extends Aliasable<SQLLinq>{
	SQLLinq select();
   
	SQLLinq select(String... columns);

	SQLLinq from(String... fromParts);

	SQLLinq from(Class<?> entityClass);
	
	SQLLinq where(Criteria criteria);
	
	SQLLinq where(Criteria criteria, String alias);
	
	SQLLinq where(Criteria criteria, String... criterions);
	
	SQLLinq where(Criteria criteria, String alias,
			String... criterions);
	
	SQLLinq add(String... criterions);

	SQLLinq orders(String... orders);

	SQLLinq setPage(Page<?> page);

	SQLLinq setSessionFactoryName(String sessionFactoryName);
	
	SQLLinq toEntity();

	SQLLinq filter(Filter filter);
	
	Page<?> paging(Page<?> page);

	Page<?> paging();
 
	<T> List<T> list();
 
	<T> T uniqueResult();
 
	int executeUpdate();
 
	SQLLinq setEntityClass(Class<?> entityClass);

	SQLLinq aliasToBean();
	
	SQLLinq aliasToBean(Class<?> cls);
	
	Page<?> paging(QueryCallback<SQLQuery> queryCallback);
	
	<T> List<T> list(QueryCallback<SQLQuery> queryCallback);
	 
	<T> T uniqueResult(QueryCallback<SQLQuery> queryCallback);
	 
	int executeUpdate(QueryCallback<SQLQuery> queryCallback);
	
	SQLLinq setEntityClass(Class<?> entityClass, boolean aliaToBean);
	
	Aliasable<SQLLinq> join(String... tables);
	 
	SQLLinq from(Class<?> entityClass, boolean aliasToBean);
	
	SQLLinq setParameter(String name, Object value);

	SQLLinq addIf(boolean need, String... criterions);

	SQLLinq addIfNotNull(Object target, String... criterions);

	SQLLinq addParser(CriterionParser<StringBuilder> criterionParser);
	
}
