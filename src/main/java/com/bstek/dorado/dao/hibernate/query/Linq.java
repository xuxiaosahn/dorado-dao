package com.bstek.dorado.dao.hibernate.query;

import java.util.Collection;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.transform.ResultTransformer;

import com.bstek.dorado.dao.hibernate.parser.CriterionParser;
import com.bstek.dorado.dao.hibernate.policy.LinqContext;
import com.bstek.dorado.data.provider.Criteria;
import com.bstek.dorado.data.provider.Page;

/**
 *@author Kevin.yang
 *@since 2015年6月12日
 */
public interface Linq {

	 Linq select(String... properties);

	 Linq select(Projection... projections);

	 Linq add(Criterion... criterions);

	 Linq where(Criterion... criterions);

	 Linq orders(Order... orders);

	 Linq aliasToBean();
	 
	 Linq toEntity();

	 Linq aliasToBean(Class<?> cls);

	 Linq setTransformer(ResultTransformer transformer);

	 Linq setPage(Page<?> page);
	 
	 Page<?> paging(Page<?> page);

	 Linq setSessionFactoryName(String sessionFactoryName);

	 DetachedCriteria detachedCriteria();

	 Linq filter(Filter filter);

	 Page<?> paging();

	 <T> List<T> list();

	 <T> T uniqueResult();
	 
	Linq addParser(CriterionParser<Criterion> criterionParser);
	
	Linq addSubQueryParser(Class<?> entityClass, String... foreignKeys);

	Linq addSubQueryParser(Class<?>... entityClasses);

	Linq where(Criteria criteria);

	Linq where(Criteria criteria, Criterion...criterions);
	
	LinqContext getLinqContext();

	Linq collect(String... properties);

	Linq collect(Class<?> entityClass, String... properties);

	Linq collectSelect(Class<?> entityClass, String... projections);

	Linq addIf(boolean need, Criterion...criterions);

	Linq addIfNotNull(Object target, Criterion...criterions);

	Linq eq(String property, Object value);

	Linq isNull(String property);

	Linq isNotNull(String property);

	Linq ne(String property, Object value);

	Linq like(String property, Object value);

	Linq like(String property, String value, MatchMode matchMode);

	Linq ilike(String property, String value, MatchMode matchMode);

	Linq ilike(String property, Object value);

	Linq in(String property, Collection<?> values);

	Linq in(String property, Object... values);

	Linq idEq(Object value);

	Linq and(Criterion lhs, Criterion rhs);

	Linq or(Criterion lhs, Criterion rhs);

	Linq not(Criterion expression);

	Linq between(String property, Object lo, Object hi);

	Linq lt(String property, Object value);

	Linq le(String property, Object value);

	Linq gt(String property, Object value);

	Linq ge(String property, Object value);

	Linq setDisableSmartSubQueryCriterion();

	Linq setDisableBackFillFilter();

	Linq setParent(Linq parent);

	Linq getParent();

	Linq eqProperty(String property, String otherProperty);

	Linq exists(Class<?> entityClass, String alias);

	Linq back();

	Linq neProperty(String property, String otherProperty);

	Linq geProperty(String property, String otherProperty);

	Linq gtProperty(String property, String otherProperty);

	Linq leProperty(String property, String otherProperty);

	Linq ltProperty(String property, String otherProperty);

	Linq isEmpty(String property);

	Linq isNotEmpty(String property);

	Linq sizeEq(String property, int size);

	Linq sizeNe(String property, int size);

	Linq sizeLt(String property, int size);

	Linq sizeLe(String property, int size);

	Linq sizeGt(String property, int size);

	Linq sizeGe(String property, int size);

	Linq notExists(Class<?> entityClass, String alias);

	Linq eq(Object value, Class<?> entityClass, String alias);

	Linq eqAll(Object value, Class<?> entityClass, String alias);

	Linq ge(Object value, Class<?> entityClass, String alias);

	Linq geAll(Object value, Class<?> entityClass, String alias);

	Linq geSome(Object value, Class<?> entityClass, String alias);

	Linq gt(Object value, Class<?> entityClass, String alias);

	Linq gtAll(Object value, Class<?> entityClass, String alias);

	Linq gtSome(Object value, Class<?> entityClass, String alias);

	Linq in(Object value, Class<?> entityClass, String alias);

	Linq le(Object value, Class<?> entityClass, String alias);

	Linq leAll(Object value, Class<?> entityClass, String alias);

	Linq leSome(Object value, Class<?> entityClass, String alias);

	Linq lt(Object value, Class<?> entityClass, String alias);

	Linq ltAll(Object value, Class<?> entityClass, String alias);

	Linq ltSome(Object value, Class<?> entityClass, String alias);

	Linq notIn(Object value, Class<?> entityClass, String alias);

	Linq propertyEq(String property, Class<?> entityClass, String alias);

	Linq propertyEqAll(String property, Class<?> entityClass, String alias);

	Linq propertyGe(String property, Class<?> entityClass, String alias);

	Linq propertyGeAll(String property, Class<?> entityClass, String alias);

	Linq propertyGeSome(String property, Class<?> entityClass, String alias);

	Linq propertyGt(String property, Class<?> entityClass, String alias);

	Linq propertyGtAll(String property, Class<?> entityClass, String alias);

	Linq propertyGtSome(String property, Class<?> entityClass, String alias);

	Linq propertyIn(String property, Class<?> entityClass, String alias);

	Linq propertyLe(String property, Class<?> entityClass, String alias);

	Linq propertyLeAll(String property, Class<?> entityClass, String alias);

	Linq propertyLeSome(String property, Class<?> entityClass, String alias);

	Linq propertyLt(String property, Class<?> entityClass, String alias);

	Linq propertyLtAll(String property, Class<?> entityClass, String alias);

	Linq propertyLtSome(String property, Class<?> entityClass, String alias);

	Linq propertyNotIn(String property, Class<?> entityClass, String alias);

	Linq or();

	Linq and();


}