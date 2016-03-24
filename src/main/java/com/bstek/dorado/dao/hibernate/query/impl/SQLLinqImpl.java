package com.bstek.dorado.dao.hibernate.query.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.transform.Transformers;
import org.springframework.util.Assert;

import com.bstek.dorado.dao.AfterWhere;
import com.bstek.dorado.dao.hibernate.HibernateUtils;
import com.bstek.dorado.dao.hibernate.parser.CriterionParser;
import com.bstek.dorado.dao.hibernate.policy.LinqContext;
import com.bstek.dorado.dao.hibernate.policy.impl.QLCriteriaContext;
import com.bstek.dorado.dao.hibernate.query.Aliasable;
import com.bstek.dorado.dao.hibernate.query.Filter;
import com.bstek.dorado.dao.hibernate.query.QueryCallback;
import com.bstek.dorado.dao.hibernate.query.SQLLinq;
import com.bstek.dorado.data.entity.EntityUtils;
import com.bstek.dorado.data.provider.And;
import com.bstek.dorado.data.provider.Criteria;
import com.bstek.dorado.data.provider.Page;

/**
 *@author Kevin.yang
 *@since 2015年6月10日
 */
public class SQLLinqImpl implements SQLLinq{
	protected StringBuilder sql = new StringBuilder();
	protected StringBuilder selectPart = new StringBuilder();
	protected StringBuilder fromPart = new StringBuilder();
	protected StringBuilder countSql = new StringBuilder();
	protected AfterWhere afterWhere = new AfterWhere();
	protected List<String> aliases = new ArrayList<String>();
	protected Page<?> page;
	protected String alias;
	protected Criteria criteria;
	private boolean toEntity;
	protected Class<?> entityClass;
	protected String sessionFactoryName;
	protected LinqContext linqContext = new LinqContext();
	protected Filter filter;
	protected Class<?> beanClass;
	private List<CriterionParser<StringBuilder>> criterionParsers = new ArrayList<CriterionParser<StringBuilder>>();

	
	private SQLLinqImpl(){
		selectPart.append("select *");
	}
	
	private SQLLinqImpl(String sql, String countSql){
		Assert.hasText(sql, "ql can not be blank");

		this.sql.append(sql);
		this.countSql.append(countSql);
	}
	
	public final static SQLLinq create() {
		return new SQLLinqImpl();
	}
	
	public final static SQLLinq forSQL(String sql) {
		return new SQLLinqImpl(sql, StringUtils.EMPTY);
	}

	public final static SQLLinq forSQL(String sql, String countSql) {
		return new SQLLinqImpl(sql, countSql);
	}

	@Override
	public SQLLinq from(Class<?> entityClass) {
		return from(entityClass, false);
	}

	@Override
 	public SQLLinq from(Class<?> entityClass, boolean aliasToBean) {
 		this.entityClass = entityClass;
 		fromPart = new StringBuilder();
 		if (aliasToBean) {
			this.beanClass = entityClass;
		}
 		fromPart.append(" from ").append(HibernateUtils.getTableName(entityClass));
 		return this;
 	}

	@Override
	public SQLLinq where(Criteria criteria, String alias, String ...criterions) {
		this.alias = alias;
		this.criteria = criteria;
		doParseCriteria();
		this.add(criterions);
		return this;
	}

	@Override
	public Page<?> paging() {
		return paging(( QueryCallback<SQLQuery> )null);
	}

	@Override
	public Page<?> paging(QueryCallback<SQLQuery> queryCallback) {
		Assert.notNull(page, "page can not be null");
		StringBuilder countSql = new StringBuilder();
		if (StringUtils.isBlank(this.countSql.toString())) {
			countSql.append("select count(*)").append(fromPart)
				.append(getWherePart());
		} else {
			countSql.append(this.countSql).append(getWherePart());
		}
		SessionFactory sessionFactory = HibernateUtils.getSessionFactory(sessionFactoryName, entityClass);
		Session session = sessionFactory.openSession();
		try {
			SQLQuery query = session.createSQLQuery(getQL());
			SQLQuery countQuery = session.createSQLQuery(countSql.toString());
			HibernateUtils.setQueryParameters(query, afterWhere.getParamMap());
			HibernateUtils.setQueryParameters(countQuery, afterWhere.getParamMap());
			if(entityClass == null) {
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
			}
			initQuery(query, sessionFactory, queryCallback);
			HibernateUtils.pagingSQLQuery(page, query, countQuery);

		} finally {
			session.close();
		}
		doFilter(page.getEntities());
		return page;
	}
	
	protected void initQuery(SQLQuery query, SessionFactory sessionFactory, QueryCallback<SQLQuery> queryCallback) {
		boolean needed =false;
		if (beanClass != null) {
			query.setResultTransformer(Transformers.aliasToBean(beanClass));
			needed = true;
		} else if(entityClass != null) {
			if (aliases.size() > 0) {
				query.setResultTransformer(Transformers.aliasToBean(entityClass));
				needed = true;
			} else if (HibernateUtils.isEntityClass(sessionFactory, entityClass)) {
				query.addEntity(entityClass);
			}
		}
		if (needed) {
			for (String alias : aliases) {
				query.addScalar(alias);
			}
		}
		if (queryCallback != null) {
			queryCallback.initQuery(query);
		}

	}

	@Override
	public <T> List<T> list() {
		return list(null);
	}

	@Override
	public <T> List<T> list(QueryCallback<SQLQuery> queryCallback) {
		List<T> list =null;
		SessionFactory sessionFactory = HibernateUtils.getSessionFactory(sessionFactoryName, entityClass);
		Session session = sessionFactory.openSession();
		try {
			SQLQuery query = session.createSQLQuery(getQL());
			HibernateUtils.setQueryParameters(query, afterWhere.getParamMap());
			initQuery(query, sessionFactory, queryCallback);
			list = HibernateUtils.queryBySQL(query);
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
	public <T> T uniqueResult(QueryCallback<SQLQuery> queryCallback) {
		Session session = HibernateUtils.getSessionFactory(sessionFactoryName, entityClass).openSession();
		try {
			SQLQuery query = session.createSQLQuery(getQL());
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
	public int executeUpdate(QueryCallback<SQLQuery> queryCallback) {
		Session session = HibernateUtils.getSessionFactory(sessionFactoryName, entityClass).openSession();
		try {
			SQLQuery query = session.createSQLQuery(getQL());
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
	public SQLLinq select() {
		selectPart = new StringBuilder("select *");
		return this;
	}

	@Override
	public SQLLinq select(String... columns) {
		selectPart = new StringBuilder();
		aliases.clear();
		for (String column : columns) {
			String[] cs = column.split("\\s*,\\s*");
			for (String c : cs) {
				String alias = StringUtils.trim(c);
				String[] ca = alias.split("\\s+[aA][sS]\\s+");
				if (ca.length > 1) {
					alias = ca[1];
				} else {
					ca = alias.split("\\s+");
					if (ca.length > 1) {
						alias = ca[1];
					}
				}
				aliases.add(alias);
			}
		}
		
 		selectPart.append("select ").append(StringUtils.join(columns, ','));
		return this;
	}

	@Override
	public SQLLinq from(String... fromParts) {
		fromPart = new StringBuilder();
 		fromPart.append(" from ").append(StringUtils.join(fromParts,','));
 		return this;
	}

	public Aliasable<SQLLinq> join(String...tables) {
 		fromPart.append(" join ").append(StringUtils.join(tables," join "));
 		return this;
 	}

	@Override
	public SQLLinq as(String alias) {
 		fromPart.append(" ").append(alias);
 		return this;
 	}

	@Override
	public SQLLinq where(Criteria criteria) {
		where(criteria, null, new String[]{});
		return this;

	}

	@Override
	public SQLLinq where(Criteria criteria, String alias) {
		where(criteria, alias, new String[]{});
		return this;
	}

	@Override
	public SQLLinq where(Criteria criteria, String... criterions) {
		where(criteria, null, criterions);
		return this;
	}

	@Override
	public SQLLinq add(String... criterions) {
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
	public SQLLinq addIf(boolean need, String... criterions) {
		if (need) {
			add(criterions);
		}
		return this;
	}
	
	@Override
	public SQLLinq addIfNotNull(Object target, String... criterions) {
		if (target != null) {
			add(criterions);
		}
		return this;
	}

	@Override
	public SQLLinq orders(String... orders) {
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
	public SQLLinq setPage(Page<?> page) {
		this.page = page;
		return this;
	}

	@Override
	public SQLLinq setSessionFactoryName(String sessionFactoryName) {
		this.sessionFactoryName = sessionFactoryName;
		return this;
	}

	@Override
	public SQLLinq filter(Filter filter) {
		this.filter = filter;
		return this;
	}

	@Override
	public SQLLinq setEntityClass(Class<?> entityClass) {
		this.entityClass = entityClass;
		return this;
	}

	@Override
	public SQLLinq aliasToBean() {
		this.beanClass = this.entityClass;
		return this;
	}

	@Override
	public SQLLinq aliasToBean(Class<?> cls) {
		this.beanClass = cls;
		return this;
	}

	@Override
	public SQLLinq setEntityClass(Class<?> entityClass, boolean aliaToBean) {
		this.entityClass = entityClass;
		if (aliaToBean) {
			this.beanClass = entityClass;
		}
		return this;
	}
	
	protected void doParseCriteria() {
		QLCriteriaContext context = new QLCriteriaContext();
		context.setAlias(alias);
		context.setEntityClass(entityClass);
		context.setCriteria(criteria);
		context.setJunction(new And());
		context.setCriterionParsers(criterionParsers);
		afterWhere = HibernateUtils.getDefaultSQLCriteriaPolicy().apply(context);
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
		if (StringUtils.isBlank(sql.toString())) {
			sb.append(selectPart).append(fromPart)
				.append(getWherePart())
				.append(afterWhere.getOrderPart());
		} else {
			sb.append(sql).append(getWherePart())
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
	public SQLLinq setParameter(String name, Object value) {
		afterWhere.getParamMap().put(name, value);
		return this;
	}

	@Override
	public SQLLinq toEntity() {
		this.toEntity =true;
		return this;
	}
	
	@Override
	public SQLLinq addParser(
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
