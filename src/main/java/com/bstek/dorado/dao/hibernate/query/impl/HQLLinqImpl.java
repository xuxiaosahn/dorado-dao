package com.bstek.dorado.dao.hibernate.query.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.util.Assert;

import com.bstek.dorado.dao.AfterWhere;
import com.bstek.dorado.dao.hibernate.HibernateUtils;
import com.bstek.dorado.dao.hibernate.parser.CriterionParser;
import com.bstek.dorado.dao.hibernate.policy.LinqContext;
import com.bstek.dorado.dao.hibernate.policy.impl.QLCriteriaContext;
import com.bstek.dorado.dao.hibernate.query.Aliasable;
import com.bstek.dorado.dao.hibernate.query.Filter;
import com.bstek.dorado.dao.hibernate.query.HQLLinq;
import com.bstek.dorado.dao.hibernate.query.QueryCallback;
import com.bstek.dorado.data.entity.EntityUtils;
import com.bstek.dorado.data.provider.And;
import com.bstek.dorado.data.provider.Criteria;
import com.bstek.dorado.data.provider.Page;

/**
 *@author Kevin.yang
 *@since 2015年6月10日
 */
public class HQLLinqImpl implements HQLLinq {
	protected StringBuilder hql = new StringBuilder();
	protected StringBuilder selectPart = new StringBuilder();
	protected StringBuilder fromPart = new StringBuilder();
	protected StringBuilder countHql = new StringBuilder();
	protected AfterWhere afterWhere = new AfterWhere();;
	protected Page<?> page;
	private boolean toEntity;
	protected Class<?> entityClass;
	protected String sessionFactoryName;
	protected LinqContext linqContext = new LinqContext();
	protected Filter filter;
	private List<CriterionParser<StringBuilder>> criterionParsers = new ArrayList<CriterionParser<StringBuilder>>();

	private HQLLinqImpl(){
	}
	
	private HQLLinqImpl(String hql, String countHql){
		Assert.hasText(hql, "hql can not be blank");

		this.hql.append(hql);
		this.countHql.append(countHql);
	}
	
	public final static HQLLinq create() {
		return new HQLLinqImpl();
	}
	
	public final static HQLLinq forHQL(String hql) {
		return new HQLLinqImpl(hql, StringUtils.EMPTY);
	}
	
	public final static HQLLinq forHQL(String hql, String countHql) {
		return new HQLLinqImpl(hql, countHql);
	}

	@Override
 	public HQLLinq from(Class<?> entityClass) {
 		this.entityClass = entityClass;
 		fromPart = new StringBuilder();
 		fromPart.append(" from ").append(entityClass.getName());
 		return this;
 	}

	@Override
	public HQLLinq where(Criteria criteria, String alias, String ...criterions) {
		QLCriteriaContext context = new QLCriteriaContext();
		context.setAlias(alias);
		context.setEntityClass(entityClass);
		context.setCriteria(criteria);
		context.setJunction(new And());
		context.setCriterionParsers(criterionParsers);
		afterWhere = HibernateUtils.getDefaultHQLCriteriaPolicy().apply(context);
		this.add(criterions);
		return this;
	}
	
	@Override
	public Page<?> paging() {
		return paging((QueryCallback<Query>) null);
	}

	@Override
	public Page<?> paging(QueryCallback<Query> queryCallback) {
		Assert.notNull(page, "page can not be null");
		StringBuilder countHql = new StringBuilder();
		if (StringUtils.isBlank(this.countHql.toString())) {
			countHql.append("select count(*)").append(fromPart)
				.append(getWherePart());
		} else {
			countHql.append(this.countHql).append(getWherePart());
		}
		Session session = HibernateUtils.getSessionFactory(sessionFactoryName, entityClass).openSession();
		try {
			Query query = session.createQuery(getQL());
			Query countQuery = session.createQuery(countHql.toString());
			HibernateUtils.setQueryParameters(query, afterWhere.getParamMap());
			HibernateUtils.setQueryParameters(countQuery, afterWhere.getParamMap());

			if (queryCallback != null) {
				queryCallback.initQuery(query);
			}
			HibernateUtils.pagingQuery(page, query, countQuery);
		} finally {
			session.close();
		}
		doFilter(page.getEntities());
		return page;
	}

	@Override
	public <T> List<T> list() {
		return list(null);
	}

	@Override
	public <T> List<T> list(QueryCallback<Query> queryCallback) {
		List<T> list =null;
		Session session = HibernateUtils.getSessionFactory(sessionFactoryName, entityClass).openSession();
		try {
			Query query = session.createQuery(getQL());
			HibernateUtils.setQueryParameters(query, afterWhere.getParamMap());
			if (queryCallback != null) {
				queryCallback.initQuery(query);
			}
			list = HibernateUtils.query(query);
			
		} finally {
			session.close();
		}
		doFilter(list);
		return list;
	}

	@Override
	public <T> T uniqueResult() { 
		return uniqueResult(null);
	}

	@Override
	public <T> T uniqueResult(QueryCallback<Query> queryCallback) {
		Session session = HibernateUtils.getSessionFactory(sessionFactoryName, entityClass).openSession();
		try {
			Query query = session.createQuery(getQL());
			HibernateUtils.setQueryParameters(query, afterWhere.getParamMap());
			if (queryCallback != null) {
				queryCallback.initQuery(query);
			}
			T t = HibernateUtils.queryUniqueResult(query);
			if (toEntity) {
				try {
					t = EntityUtils.toEntity(t);
				} catch (Exception e) {
					throw new RuntimeException(e.getMessage());
				}
			}
			if (this.filter != null ) {
				linqContext.setEntity(t);
				if (!this.filter.invoke(linqContext)) {
					return null;
				}
			}
			return t;
		} finally {
			session.close();
		}
	}

	@Override
	public int executeUpdate() {
		return executeUpdate(null);
	}

	@Override
	public int executeUpdate(QueryCallback<Query> queryCallback) {
		Session session = HibernateUtils.getSessionFactory(sessionFactoryName, entityClass).openSession();
		try {
			Query query = session.createQuery(getQL());
			HibernateUtils.setQueryParameters(query, afterWhere.getParamMap());
			if (queryCallback != null) {
				queryCallback.initQuery(query);
			}
			return query.executeUpdate();		
		} finally {
			session.close();
		}
	}

	@Override
	public HQLLinq select() {
		selectPart = new StringBuilder();
		return this;
	}

	@Override
	public HQLLinq select(String... properties) {
		selectPart = new StringBuilder();
 		selectPart.append("select ").append(StringUtils.join(properties, ','));
		return this;

	}

	@Override
	public HQLLinq from(String... fromParts) {
		fromPart = new StringBuilder();
 		fromPart.append(" from ").append(StringUtils.join(fromParts,','));
 		return this;
	}

	public Aliasable<HQLLinq> join(String...tables) {
 		fromPart.append(" join ").append(StringUtils.join(tables," join "));
 		return this;
 	}

	@Override
	public HQLLinq as(String alias) {
 		fromPart.append(" ").append(alias);
 		return this;
 	}

	@Override
	public HQLLinq where(Criteria criteria) {
		where(criteria, null, new String[]{});
		return this;
	}

	@Override
	public HQLLinq where(Criteria criteria, String alias) {
		where(criteria, alias, new String[]{});
		return this;
	}

	@Override
	public HQLLinq where(Criteria criteria, String... criterions) {
		where(criteria, null, criterions);
		return this;
	}

	@Override
	public HQLLinq add(String... criterions) {
		if (criterions.length >0) {
			if (StringUtils.isBlank(afterWhere.getWherePart().toString())) {
			 	afterWhere.getWherePart().append(" where ").append(StringUtils.join(criterions," and "));
			} else {
			 	afterWhere.getWherePart().append(" and ").append(StringUtils.join(criterions," and "));
			}
		}
		return this;
	}
	
	@Override
	public HQLLinq addIf(boolean need, String... criterions) {
		if (need) {
			add(criterions);
		}
		return this;
	}
	
	@Override
	public HQLLinq addIfNotNull(Object target, String... criterions) {
		if (target != null) {
			add(criterions);
		}
		return this;
	}

	@Override
	public HQLLinq orders(String... orders) {
		StringBuilder orderPart = afterWhere.getOrderPart();
		if (orders.length >0) {
			if (StringUtils.isBlank(orderPart.toString())) {
				orderPart.append(" order by ")
					.append(StringUtils.join(orders, ','));
			} else {
				orderPart.append(",")
					.append(StringUtils.join(orders, ','));
			}
		}
		return this;
	}

	@Override
	public HQLLinq setPage(Page<?> page) {
		this.page = page;
		return this;
	}

	@Override
	public HQLLinq setSessionFactoryName(String sessionFactoryName) {
		this.sessionFactoryName = sessionFactoryName;
		return this;
	}

	@Override
	public HQLLinq filter(Filter filter) {
		this.filter = filter;
		return this;
	}

	@Override
	public HQLLinq setEntityClass(Class<?> entityClass) {
		this.entityClass = entityClass;
		return this;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void doFilter(Collection list) {
		if (toEntity) {
			Collection copy = new ArrayList(list.size());
			copy.addAll(list);
			list.clear();
			for (Object entity : copy) {
				try {
					list.add(EntityUtils.toEntity(entity));
				} catch (Exception e) {
					throw new RuntimeException(e.getMessage());
				}
			}
		}
		if (filter != null) {
			Iterator<?> iterator = list.iterator();
			while (iterator.hasNext()) {
				Object entity = iterator.next();
				linqContext.setEntity(entity);
				if (!filter.invoke(linqContext)) {
					iterator.remove();
				}
			}
		}
	}
	
	protected String getQL() {
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isBlank(hql.toString())) {
			sb.append(selectPart).append(fromPart)
				.append(getWherePart())
				.append(afterWhere.getOrderPart());
		} else {
			sb.append(hql).append(fromPart).append(getWherePart())
				.append(afterWhere.getOrderPart());
		}
		return sb.toString();
	}
	
	protected String getWherePart() {
		String wherePart = afterWhere.getWherePart().toString();
		if(AfterWhere.START_WHERE.equals(wherePart)) {
			return StringUtils.EMPTY;
		}
		return wherePart;
	}

	@Override
	public HQLLinq setParameter(String name, Object value) {
		afterWhere.getParamMap().put(name, value);
		return this;
	}

	@Override
	public HQLLinq toEntity() {
		this.toEntity =true;
		return this;
	}

	@Override
	public HQLLinq addParser(
			CriterionParser<StringBuilder> criterionParser) {
		this.criterionParsers.add(criterionParser);
		return this;
	}

	@Override
	public Page<?> paging(Page<?> page) {
		this.page = page;
		return paging();
	}
}
