package com.bstek.dorado.dao.hibernate;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Property;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.bstek.dorado.core.Configure;
import com.bstek.dorado.dao.AfterWhere;
import com.bstek.dorado.dao.BeanUtils;
import com.bstek.dorado.dao.Constants;
import com.bstek.dorado.dao.GenricTypeUtils;
import com.bstek.dorado.dao.hibernate.policy.CriteriaPolicy;
import com.bstek.dorado.dao.hibernate.policy.SaveContext;
import com.bstek.dorado.dao.hibernate.policy.SavePolicy;
import com.bstek.dorado.dao.hibernate.policy.impl.DeleteSavePolicy;
import com.bstek.dorado.dao.hibernate.policy.impl.DirtyTreeSavePolicy;
import com.bstek.dorado.dao.hibernate.policy.impl.QBCCriteriaContext;
import com.bstek.dorado.dao.hibernate.policy.impl.QLCriteriaContext;
import com.bstek.dorado.dao.hibernate.policy.impl.SimpleSavePolicy;
import com.bstek.dorado.dao.hibernate.policy.impl.UpdateSavePolicy;
import com.bstek.dorado.dao.hibernate.query.HQLLinq;
import com.bstek.dorado.dao.hibernate.query.Linq;
import com.bstek.dorado.dao.hibernate.query.SQLLinq;
import com.bstek.dorado.dao.hibernate.query.impl.HQLLinqImpl;
import com.bstek.dorado.dao.hibernate.query.impl.LinqImpl;
import com.bstek.dorado.dao.hibernate.query.impl.SQLLinqImpl;
import com.bstek.dorado.data.provider.And;
import com.bstek.dorado.data.provider.Criteria;
import com.bstek.dorado.data.provider.Page;

/**
 *@author Kevin.yang
 *@since 2015年5月16日
 */
public class HibernateUtils implements ApplicationContextAware {
	
	private static ApplicationContext applicationContext;
	private static SessionFactory defaultSessionFactory;
	private static Map<String, SessionFactory> sessionFactoryMap;
	private static SavePolicy defaultSavePolicy;
	private static CriteriaPolicy<DetachedCriteria> defaultQBCCriteriaPolicy;
	private static CriteriaPolicy<AfterWhere> defaultSQLCriteriaPolicy;
	private static CriteriaPolicy<AfterWhere> defaultHQLCriteriaPolicy;
	
	
	public static CriteriaPolicy<DetachedCriteria> getDefaultQBCCriteriaPolicy() {
		return defaultQBCCriteriaPolicy;
	}
	
	public static CriteriaPolicy<AfterWhere> getDefaultHQLCriteriaPolicy() {
		return defaultHQLCriteriaPolicy;
	}
	
	public static CriteriaPolicy<AfterWhere> getDefaultSQLCriteriaPolicy() {
		return defaultSQLCriteriaPolicy;
	}
	
	public static Linq createLinq(Class<?> entityClass) {
		return LinqImpl.forClass(entityClass);
	} 
	
	/**
	 * 创建基于QBC的Linq查询或者更新。
	 * 
	 * @param entityClass 实体类
	 * @param alias 别名
	 * @return Linq查询对象
	 */
	public static Linq createLinq(Class<?> entityClass, String alias) {
		return LinqImpl.forClass(entityClass, alias);
	}
	
	public static HQLLinq createHQLLinq() {
		return HQLLinqImpl.create();
	} 
	
	public static HQLLinq createHQLLinq(String hql) {
		return HQLLinqImpl.forHQL(hql);
	}
	
	/**
	 * 创建基于HQL的Linq查询或者更新。
	 *  
	 * @param hql 数据查询语句
	 * @param countHql 数据总条数查询语句
	 * @return HQLLinq查询对象
	 */
	public static HQLLinq createHQLLinq(String hql, String countHql) {
		return HQLLinqImpl.forHQL(hql, countHql);
	}
	
	public static SQLLinq createSQLLinq() {
		return SQLLinqImpl.create();
	} 
	
	public static SQLLinq createSQLLinq(String sql) {
		return SQLLinqImpl.forSQL(sql);
	}
	
	/**
	 * 创建基于SQL的Linq查询或者更新。
	 * 
	 * @param sql 数据查询语句
	 * @param countSql 数据总条数查询语句
	 * @return SQLLinq查询对象
	 */
	public static SQLLinq createSQLLinq(String sql, String countSql) {
		return SQLLinqImpl.forSQL(sql, countSql);
	}
	
	public static void save(Object entityOrEntityList) {
		save(entityOrEntityList, StringUtils.EMPTY);
	}
	
	/**
	 * 智能数据保存。
	 * <p>
	 * sessionFactoryName一般情况不需要指定，方法内部会根据实体类智能判断所属SessionFactory，<br/>
	 * 只有当某个实体类归属于多个SessionFactory的情况下，需要明确指定SessionFactory名称。<br/>
	 * 同时智能提取立体数据模型中的各个层级的实体数据，交由系统默认savePolicy来处理后继的持久化操作。
	 * </p>
	 * 
	 * @param entityOrEntityList 实体类对象或者实体类集合
	 * @param sessionFactoryName SessionFactory名称
	 */
	public static void save(Object entityOrEntityList, String sessionFactoryName) {
		Class<?> entityClass = GenricTypeUtils.getGenricType(entityOrEntityList);
		Session session = getSession(sessionFactoryName, entityClass);
		try {
			SaveContext context = new SaveContext();
			context.setEntity(entityOrEntityList);
			context.setSession(session);
			defaultSavePolicy.apply(context);
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}
	
	public static void save(Object entityOrEntityList, SavePolicy savePolicy) {
		save(entityOrEntityList, savePolicy, null);
	}
	
	public static void simpleSave(Object entityOrEntityList) {
		save(entityOrEntityList, new SimpleSavePolicy(), null);
	}
	
	public static void simpleSave(Object entityOrEntityList, String sessionFactoryName) {
		save(entityOrEntityList, new SimpleSavePolicy(), sessionFactoryName);
	}
	
	public static void delete(Object entityOrEntityList) {
		save(entityOrEntityList, new DeleteSavePolicy(), null);
	}
	
	public static void delete(Object entityOrEntityList, String sessionFactoryName) {
		save(entityOrEntityList, new DeleteSavePolicy(), sessionFactoryName);
	}
	
	public static void update(Object entityOrEntityList) {
		save(entityOrEntityList, new UpdateSavePolicy(), null);
	}
	
	public static void update(Object entityOrEntityList, String sessionFactoryName) {
		save(entityOrEntityList, new UpdateSavePolicy(), sessionFactoryName);
	}
	
	
	/**
	 * 智能数据保存。
	 * <p>
	 * sessionFactoryName一般情况不需要指定，方法内部会根据实体类智能判断所属SessionFactory，<br/>
	 * 只有当某个实体类归属于多个SessionFactory的情况下，需要明确指定SessionFactory名称。<br/>
	 * 同时智能提取立体数据模型中的各个层级的实体数据，交由参数savePolicy来处理后继的持久化操作。
	 * </p>
	 * 
	 * @param entityOrEntityList 实体类对象或者实体类集合
	 * @param savePolicy 保存策略
	 * @param sessionFactoryName SessionFactory名称
	 */
	public static void save(Object entityOrEntityList, SavePolicy savePolicy, String sessionFactoryName) {
		Assert.notNull(savePolicy, "savePolicy can not be null!");;
		Class<?> entityClass = GenricTypeUtils.getGenricType(entityOrEntityList);
		Session session = getSession(sessionFactoryName, entityClass);
		try {
			DirtyTreeSavePolicy dirtyTreeSavePolicy = new DirtyTreeSavePolicy();
			dirtyTreeSavePolicy.setSavePolicy(savePolicy);
			SaveContext context = new SaveContext();
			context.setEntity(entityOrEntityList);
			context.setSession(session);
			dirtyTreeSavePolicy.apply(context);
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}
	
	public static DetachedCriteria createFilter(Class<?> entityClass) {
		return createFilter(null, entityClass, StringUtils.EMPTY);
	}
	
	public static DetachedCriteria createFilter(Class<?> entityClass, String alias) {
		return createFilter(null, entityClass, alias);
	}

	public static DetachedCriteria createFilter(Criteria criteria, Class<?> entityClass) {
		return createFilter(criteria, entityClass, StringUtils.EMPTY);
	}
	
	/**
	 * 创建QBC查询对象。
	 * 
	 * @param criteria
	 * @param entityClass
	 * @param alias
	 * @return QBC查询对象
	 */
	public static DetachedCriteria createFilter(Criteria criteria, Class<?> entityClass, String alias) {
		QBCCriteriaContext context = new QBCCriteriaContext();
		context.setAlias(alias);
		context.setCriteria(criteria);
		context.setEntityClass(entityClass);
		return defaultQBCCriteriaPolicy.apply(context);
	}
	
	public static void pagingQuery(Page<?> page, Criteria criteria, Class<?> entityClass) {
		pagingQuery(page, criteria, entityClass, false);
	}
	
	public static void pagingQuery(Page<?> page, Criteria criteria, Class<?> entityClass, boolean aliasToBean) {
		DetachedCriteria dc = createFilter(criteria, entityClass, null);
		if (aliasToBean) {
			dc.setResultTransformer(Transformers.aliasToBean(entityClass));
		}
		pagingQuery(page, dc, null);
	}

	public static void pagingQuery(Page<?> page, DetachedCriteria detachedCriteria) {
		pagingQuery(page, detachedCriteria, null);
	}
	
	/**
	 * 基于QBC分页查询。
	 * 
	 * @param page Dorado 分页对象
	 * @param detachedCriteria QBC查询对象
	 * @param sessionFactoryName SessionFactory名称
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void pagingQuery(Page<?> page, DetachedCriteria detachedCriteria, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName);
		try {
			org.hibernate.Criteria criteria = detachedCriteria.getExecutableCriteria(session);
			List orderEntrys = BeanUtils.getFieldValue(criteria, "orderEntries");
			BeanUtils.setFieldValue(criteria, "orderEntries", Collections.EMPTY_LIST);
			Projection projection = BeanUtils.getFieldValue(criteria, "projection");
			ResultTransformer transformer = BeanUtils.getFieldValue(criteria, "resultTransformer");
			
			int total = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			page.setEntityCount(total);

			BeanUtils.setFieldValue(criteria, "orderEntries", orderEntrys);

			criteria.setProjection(projection);
			if (projection == null) {
				criteria.setResultTransformer(CriteriaSpecification.ROOT_ENTITY);
			}
			if (transformer != null) {
				criteria.setResultTransformer(transformer);
			}

			int start = (page.getPageNo() - 1) * page.getPageSize();
			page.setEntities(criteria.setFirstResult(start).setMaxResults(page.getPageSize()).list());
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}
	
	public static void pagingQuery(Page<?> page, String hql, String countHql, Criteria criteria) {
		pagingQuery(page, hql, countHql, criteria, null, null, null);
	}
	
	public static void pagingQuery(Page<?> page, String hql, String countHql, Criteria criteria, String sessionFactoryName) {
		pagingQuery(page, hql, countHql, criteria, null, null, sessionFactoryName);
	}
	
	public static void pagingQuery(Page<?> page, String hql, String countHql, String alias, Criteria criteria) {
		pagingQuery(page, hql, countHql, criteria, null, alias, null);
	}
	
	public static void pagingQuery(Page<?> page, String hql, String countHql, Criteria criteria, Class<?> entityClass) {
		pagingQuery(page, hql, countHql, criteria, entityClass, null, null);
	}
	
	/**
	 * 基于HQL分页查询。
	 * 
	 * @param page Dorado 分页对象
	 * @param hql 数据查询语句
	 * @param countHql 数据总纪录数查询语句
	 * @param criteria Dorado 条件对象
	 * @param entityClass 实体类
	 * @param alias 别名
	 * @param sessionFactoryName SessionFactory名称
	 */
	public static void pagingQuery(Page<?> page, String hql, String countHql, Criteria criteria, Class<?> entityClass, String alias, String sessionFactoryName) {
		QLCriteriaContext context = new QLCriteriaContext();
		context.setAlias(alias);
		context.setEntityClass(entityClass);
		context.setCriteria(criteria);
		context.setJunction(new And());
		AfterWhere afterWhere = defaultHQLCriteriaPolicy.apply(context);
		
		String wherePart = afterWhere.getWherePart().toString();
		if(AfterWhere.START_WHERE.equals(wherePart)) {
			wherePart = StringUtils.EMPTY;
		}

		pagingQuery(page, hql + wherePart + afterWhere.getOrderPart(), countHql + afterWhere.getWherePart(), afterWhere.getParamMap(), sessionFactoryName);
	}
	
	public static void pagingQuery(Page<?> page, String hql, String countHql) {
		pagingQuery(page, hql, countHql, null, StringUtils.EMPTY);
	}
	
	public static void pagingQuery(Page<?> page, String hql, String countHql, Object[] parameters) {
		pagingQuery(page, hql, countHql, parameters, null);
	}
	
	public static void pagingQuery(Page<?> page, String hql, String countHql, Map<String, Object> parameters) {
		pagingQuery(page, hql, countHql, parameters, null);
	}
	
	public static void pagingQuery(Page<?> page, String hql, String countHql, String sessionFactoryName) {
		pagingQuery(page, hql, countHql, null, sessionFactoryName);
	}

	/**
	 * 基于HQL分页查询
	 * 
	 * @param page Dorado 分页对象
	 * @param hql 数据查询语句
	 * @param countHql 数据总纪录数查询语句
	 * @param parameters 查询参数，支持Object[]和Map类型
	 * @param sessionFactoryName SessionFactory名称
	 */
	public static void pagingQuery(Page<?> page, String hql, String countHql, Object parameters, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName);
		try {
			Query query = session.createQuery(hql);
			Query countQuery = session.createQuery(countHql);
			pagingQuery(page, query, countQuery, parameters, sessionFactoryName);
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Criteria criteria) {
		pagingSQLQuery(page, sql, countSql, criteria, null, null, null);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Criteria criteria, String sessionFactoryName) {
		pagingSQLQuery(page, sql, countSql, criteria, null, null, sessionFactoryName);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, String alias, Criteria criteria) {
		pagingSQLQuery(page, sql, countSql, criteria, null, alias, null);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Criteria criteria, Class<?> entityClass) {
		pagingSQLQuery(page, sql, countSql, criteria, entityClass, null, null);
	}
	
	/**
	 * 基于SQL分页查询。
	 * 
	 * @param page Dorado 分页对象
	 * @param sql 数据查询语句
	 * @param countSql 数据总纪录数查询语句
	 * @param criteria Dorado 条件对象
	 * @param entityClass 实体类
	 * @param alias 别名
	 * @param sessionFactoryName sessionFactory名称
	 */
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Criteria criteria, Class<?> entityClass, String alias, String sessionFactoryName) {
		QLCriteriaContext context = new QLCriteriaContext();
		context.setAlias(alias);
		context.setEntityClass(entityClass);
		context.setCriteria(criteria);
		context.setJunction(new And());
		AfterWhere afterWhere = defaultSQLCriteriaPolicy.apply(context);

		String wherePart = afterWhere.getWherePart().toString();
		if(AfterWhere.START_WHERE.equals(wherePart)) {
			wherePart = StringUtils.EMPTY;
		}

		pagingSQLQuery(page, sql + afterWhere.getWherePart() + afterWhere.getOrderPart(), countSql + afterWhere.getWherePart(), afterWhere.getParamMap(), entityClass, sessionFactoryName);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql) {
		pagingSQLQuery(page, sql, countSql, null, null, null);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql,String sessionFactoryName) {
		pagingSQLQuery(page, sql, countSql, null, null, sessionFactoryName);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Object[] parameters) {
		pagingSQLQuery(page, sql, countSql, parameters, null, null);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Map<String, Object> parameters) {
		pagingSQLQuery(page, sql, countSql, parameters, null, null);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Object[] parameters, String sessionFactoryName) {
		pagingSQLQuery(page, sql, countSql, parameters, null, sessionFactoryName);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Map<String, Object> parameters, String sessionFactoryName) {
		pagingSQLQuery(page, sql, countSql, parameters, null, sessionFactoryName);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Class<?> entityClass) {
		pagingSQLQuery(page, sql, countSql, null, entityClass, null);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Object[] parameters, Class<?> entityClass) {
		pagingSQLQuery(page, sql, countSql, parameters, entityClass, null);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Class<?> entityClass, String sessionFactoryName) {
		pagingSQLQuery(page, sql, countSql, null, entityClass, sessionFactoryName);
	}
	
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Map<String, Object> parameters, Class<?> entityClass) {
		pagingSQLQuery(page, sql, countSql, parameters, entityClass, null);
	}
	
	/**
	 * 基于SQL分页查询。
	 * 
	 * @param page Dorado 分页对象
	 * @param sql 数据查询语句
	 * @param countSql 数据总纪录数查询语句
	 * @param parameters 查询参数，支持Object[]和Map类型
	 * @param entityClass 实体类
	 * @param sessionFactoryName SessionFactory名称
	 */
	public static void pagingSQLQuery(Page<?> page, String sql, String countSql, Object parameters, Class<?> entityClass, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName, entityClass);
		try {
			SQLQuery query = session.createSQLQuery(sql);
			SQLQuery countQuery = session.createSQLQuery(countSql);
			if(entityClass == null) {
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
			}
			pagingSQLQuery(page, query, countQuery, parameters, entityClass, sessionFactoryName);
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}
	
	public static void pagingQuery(Page<?> page, Query query, Query countQuery) {
		pagingQuery(page, query, countQuery, null, null);
	}
	
	public static void pagingQuery(Page<?> page, Query query, Query countQuery, String sessionFactoryName) {
		pagingQuery(page, query, countQuery, null, sessionFactoryName);
	}
	
	public static void pagingQuery(Page<?> page, Query query, Query countQuery, Object[] parameters) {
		pagingQuery(page, query, countQuery, parameters, null);
	}
	
	public static void pagingQuery(Page<?> page, Query query, Query countQuery, Map<String, Object> parameters) {
		pagingQuery(page, query, countQuery, parameters, null);
	}
	
	/**
	 * 基于Query对象分页查询。
	 * 
	 * @param page Dorado 分页对象
	 * @param query 数据查询对象
	 * @param countQuery 数据总纪录数查询对象
	 * @param parameters 查询参数，支持Object[]和Map类型
	 * @param sessionFactoryName SessionFactory名称
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void pagingQuery(Page<?> page, Query query, Query countQuery, Object parameters, String sessionFactoryName) {
		int safePageSize = page.getPageSize() < 1 ? Integer.MAX_VALUE : page.getPageSize();
		int start = (page.getPageNo() - 1) * safePageSize;
		query.setMaxResults(safePageSize).setFirstResult(start);
		page.setEntities( (Collection) query(query, parameters, sessionFactoryName));
		int count =  queryCount(countQuery, parameters, sessionFactoryName);
		page.setEntityCount(count);
	}
	
	public static void pagingSQLQuery(Page<?> page, SQLQuery query, SQLQuery countQuery) {
		pagingSQLQuery(page, query, countQuery, null, null, null);
	}
	
	public static void pagingSQLQuery(Page<?> page, SQLQuery query, SQLQuery countQuery, String sessionFactoryName) {
		pagingSQLQuery(page, query, countQuery, null, null, sessionFactoryName);
	}
	
	public static void pagingSQLQuery(Page<?> page, SQLQuery query, SQLQuery countQuery, Object[] parameters) {
		pagingSQLQuery(page, query, countQuery, parameters, null, null);
	}
	
	public static void pagingSQLQuery(Page<?> page, SQLQuery query, SQLQuery countQuery, Map<String, Object> parameters) {
		pagingSQLQuery(page, query, countQuery, parameters, null, null);
	}
	
	public static void pagingSQLQuery(Page<?> page, SQLQuery query, SQLQuery countQuery, Class<?> entityClass) {
		pagingSQLQuery(page, query, countQuery, null, entityClass, null);
	}
	
	public static void pagingSQLQuery(Page<?> page, SQLQuery query, SQLQuery countQuery, Class<?> entityClass, String sessionFactoryName) {
		pagingSQLQuery(page, query, countQuery, null, entityClass, sessionFactoryName);
	}
	
	public static void pagingSQLQuery(Page<?> page, SQLQuery query, SQLQuery countQuery, Object[] parameters, Class<?> entityClass) {
		pagingSQLQuery(page, query, countQuery, parameters, entityClass, null);
	}
	
	public static void pagingSQLQuery(Page<?> page, SQLQuery query, SQLQuery countQuery, Map<String, Object> parameters, Class<?> entityClass) {
		pagingSQLQuery(page, query, countQuery, parameters, entityClass, null);
	}
	
	/**
	 * 基于SQLQuery对象分页查询。
	 * 
	 * @param page Dorado 分页对象
	 * @param query 数据查询对象
	 * @param countQuery 数据总纪录数查询对象
	 * @param parameters 查询参数，支持Object[]和Map类型
	 * @param entityClass 实体类
	 * @param sessionFactoryName SessionFactory名称
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void pagingSQLQuery(Page<?> page, SQLQuery query, SQLQuery countQuery, Object parameters, Class<?> entityClass, String sessionFactoryName) {
		int safePageSize = page.getPageSize() < 1 ? Integer.MAX_VALUE : page.getPageSize();
		int start = (page.getPageNo() - 1) * safePageSize;
		query.setMaxResults(safePageSize).setFirstResult(start);
		page.setEntities( (Collection) queryBySQL(query, parameters, entityClass, false, sessionFactoryName));
		Integer count = queryCount(countQuery, parameters, sessionFactoryName);
		page.setEntityCount(count);
	}
	
	public static <T> List<T> query(DetachedCriteria detachedCriteria) {
		return query(detachedCriteria, null);
	}
	
	/**
	 * 基于QBC查询。
	 * 
	 * @param detachedCriteria QBC查询对象
	 * @param sessionFactoryName SessionFactory名称
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> query(DetachedCriteria detachedCriteria, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName);
		try {
			return detachedCriteria.getExecutableCriteria(session).list();
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}
	
	public static <T> List<T> query(String hql) {
		return query(hql, null, null);
	}
	
	public static <T> List<T> query(String hql, String sessionFactoryName) {
		return query(hql, null, sessionFactoryName);
	}
	
	public static <T> List<T> query(String hql, Object[] parameters) {
		return query(hql, parameters, null);
	}
	
	public static <T> List<T> query(String hql, Map<String, Object> parameters) {
		return query(hql, parameters, null);
	}
	
	/**
	 * 基于HQL查询。
	 * 
	 * @param hql 查询语句
	 * @param parameters 查询参数，支持Object[]和Map类型
	 * @param sessionFactoryName SessionFactory名称
	 * @return
	 */
	public static <T> List<T> query(String hql, Object parameters, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName);
		try {
			Query query = session.createQuery(hql);
			return query(query, parameters, sessionFactoryName);
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}
	
	public static <T> List<T> query(Query query) {
		return query(query, null, null);
	}
	
	public static <T> List<T> query(Query query, String sessionFactoryName) {
		return query(query, null, sessionFactoryName);
	}
	
	public static <T> List<T> query(Query query, Object[] parameters) {
		return query(query, parameters, null);
	}
	
	public static <T> List<T> query(Query query, Map<String, Object> parameters) {
		return query(query, parameters, null);
	}
	
	/**
	 * 基于Query对象查询。
	 * 
	 * @param query 查询对象
	 * @param parameters 查询参数，支持Object[]和Map类型
	 * @param sessionFactoryName SessionFactory名称
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> query(Query query, Object parameters, String sessionFactoryName) {
		if (parameters instanceof Object[]) {
			setQueryParameters(query, (Object[]) parameters);
		} else {
			setQueryParameters(query, (Map<String, Object>) parameters);
		}
		return query.list();

	}
	
	public static <T> List<T> queryBySQL(String sql) {
		return queryBySQL(sql, null, null, false, null);
	}
	
	public static <T> List<T> queryBySQL(String sql, String sessionFactoryName) {
		return queryBySQL(sql, null, null, false, sessionFactoryName);
	}
	
	public static <T> List<T> queryBySQL(String sql, Object[] parameters) {
		return queryBySQL(sql, parameters, null, false, null);
	}
	
	public static <T> List<T> queryBySQL(String sql,Map<String, Object> parameters) {
		return queryBySQL(sql, parameters, null, false, null);
	}
	
	public static <T> List<T> queryBySQL(String sql, Object[] parameters, String sessionFactoryName) {
		return queryBySQL(sql, parameters, null, false, sessionFactoryName);
	}
	
	public static <T> List<T> queryBySQL(String sql, Map<String, Object> parameters, String sessionFactoryName) {
		return queryBySQL(sql, parameters, null, false, sessionFactoryName);
	}
	
	public static <T> List<T> queryBySQL(String sql, Class<?> entityClass) {
		return queryBySQL(sql, null, entityClass, false, null);
	}
	
	public static <T> List<T> queryBySQL(String sql, Class<?> entityClass, boolean aliasToBean) {
		return queryBySQL(sql, null, entityClass, aliasToBean, null);
	}
	
	public static <T> List<T> queryBySQL(String sql, Class<?> entityClass, String sessionFactoryName) {
		return queryBySQL(sql, null, entityClass, false, sessionFactoryName);
	}
	
	public static <T> List<T> queryBySQL(String sql, Class<?> entityClass, boolean aliasToBean, String sessionFactoryName) {
		return queryBySQL(sql, null, entityClass, aliasToBean, sessionFactoryName);
	}
	
	public static <T> List<T> queryBySQL(String sql, Object[] parameters, Class<?> entityClass) {
		return queryBySQL(sql, parameters, entityClass, false, null);
	}
	
	public static <T> List<T> queryBySQL(String sql, Object[] parameters, Class<?> entityClass, boolean aliasToBean) {
		return queryBySQL(sql, parameters, entityClass, aliasToBean, null);
	}
	
	public static <T> List<T> queryBySQL(String sql, Map<String, Object> parameters, Class<?> entityClass) {
		return queryBySQL(sql, parameters, entityClass, false, null);
	}
	
	public static <T> List<T> queryBySQL(String sql, Map<String, Object> parameters, Class<?> entityClass, boolean aliasToBean) {
		return queryBySQL(sql, parameters, entityClass, aliasToBean, null);
	}
	
	/**
	 * 基于SQL查询。
	 * 
	 * @param sql 数据查询语句
	 * @param parameters 查询参数，支持Object[]和Map类型
	 * @param entityClass 实体类
	 * @param aliasToBean 是否按别名转换为Bean对象
	 * @param sessionFactoryName SessionFactory名称
	 * @return
	 */
	public static <T> List<T> queryBySQL(String sql, Object parameters, Class<?> entityClass, boolean aliasToBean, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName);
		try {
			SQLQuery query = session.createSQLQuery(sql);
			return queryBySQL(query, parameters, entityClass, false, sessionFactoryName);
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}

	}
	
	public static <T> List<T> queryBySQL(SQLQuery query) {
		return queryBySQL(query, null, null, false, null);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query, String sessionFactoryName) {
		return queryBySQL(query, null, null, false, sessionFactoryName);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query, Object[] parameters) {
		return queryBySQL(query, parameters, null, false, null);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query,Map<String, Object> parameters) {
		return queryBySQL(query, parameters, null, false, null);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query, Object[] parameters, String sessionFactoryName) {
		return queryBySQL(query, parameters, null, false, sessionFactoryName);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query,Map<String, Object> parameters, String sessionFactoryName) {
		return queryBySQL(query, parameters, null, false, sessionFactoryName);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query, Class<?> entityClass) {
		return queryBySQL(query, null, entityClass, false, null);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query, Class<?> entityClass, boolean aliasToBean) {
		return queryBySQL(query, null, entityClass, aliasToBean, null);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query, Class<?> entityClass, String sessionFactoryName) {
		return queryBySQL(query, null, entityClass, false, sessionFactoryName);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query, Class<?> entityClass, boolean aliasToBean, String sessionFactoryName) {
		return queryBySQL(query, null, entityClass, aliasToBean, sessionFactoryName);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query, Object[] parameters, Class<?> entityClass) {
		return queryBySQL(query, parameters, entityClass, false, null);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query, Object[] parameters, Class<?> entityClass, boolean aliasToBean) {
		return queryBySQL(query, parameters, entityClass, aliasToBean, null);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query, Map<String, Object> parameters, Class<?> entityClass) {
		return queryBySQL(query, parameters, entityClass, false, null);
	}
	
	public static <T> List<T> queryBySQL(SQLQuery query, Map<String, Object> parameters, Class<?> entityClass, boolean aliasToBean) {
		return queryBySQL(query, parameters, entityClass, aliasToBean, null);
	}
	
	/**
	 * 基于SQLQuery查询。
	 * 
	 * @param query 数据查询语句
	 * @param parameters 查询参数，支持Object[]和Map类型
	 * @param entityClass 实体类
	 * @param aliasToBean 是否按别名转换为Bean对象
	 * @param sessionFactoryName SessionFactory名称
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> queryBySQL(SQLQuery query, Object parameters, Class<?> entityClass, boolean aliasToBean, String sessionFactoryName) {
		if (entityClass != null) {
			if (aliasToBean) {
				query.setResultTransformer(Transformers.aliasToBean(entityClass));
			} else if (isEntityClass(getSessionFactory(sessionFactoryName, entityClass), entityClass)) {
				query.addEntity(entityClass);
			} else {
				query.setResultTransformer(Transformers.aliasToBean(entityClass));
			}
		}
		if (parameters instanceof Object[]) {
			setQueryParameters(query, (Object[]) parameters);
		} else {
			setQueryParameters(query, (Map<String, Object>) parameters);
		}
		return query.list();

	}
	
	public static <T> List<T> query(DetachedCriteria detachedCriteria, int pageIndex, int pageSize) {
		return query(detachedCriteria, pageIndex, pageSize, null);
	}

	/**
	 *  基于QBC查询（带分页）。
	 * 
	 * @param detachedCriteria
	 * @param pageIndex
	 * @param pageSize
	 * @param sessionFactoryName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> query(DetachedCriteria detachedCriteria, int pageIndex, int pageSize, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName);
		try {
			return detachedCriteria.getExecutableCriteria(session).setFirstResult((pageIndex - 1) * pageSize)
					.setMaxResults(pageSize).list();
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}
	
	public static int queryCount(DetachedCriteria detachedCriteria) {
		return queryCount(detachedCriteria, null);
	}
	
	/**
	 * 基于QBC查询数据条数。
	 * 
	 * @param detachedCriteria
	 * @param sessionFactoryName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static int queryCount(DetachedCriteria detachedCriteria, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName);
		try {
			org.hibernate.Criteria criteria = detachedCriteria.getExecutableCriteria(session);
			List orderEntrys = BeanUtils.getFieldValue(criteria, "orderEntries");
			BeanUtils.setFieldValue(criteria, "orderEntries", Collections.EMPTY_LIST);
			Projection projection = BeanUtils.getFieldValue(criteria, "projection");
			ResultTransformer transformer = BeanUtils.getFieldValue(criteria, "resultTransformer");
			
			int total = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).intValue();

			BeanUtils.setFieldValue(criteria, "orderEntries", orderEntrys);

			criteria.setProjection(projection);
			if (projection == null) {
				criteria.setResultTransformer(CriteriaSpecification.ROOT_ENTITY);
			}
			if (transformer != null) {
				criteria.setResultTransformer(transformer);
			}
			return total;

		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}
	
	public static int queryCountBySQL(String sql) {
		return queryCountBySQL(sql, null, null);
	}
	
	public static int queryCountBySQL(String sql, String sessionFactoryName) {
		return queryCountBySQL(sql, null, sessionFactoryName);
	}

	public static int queryCountBySQL(String sql, Object[] parameters) {
		return queryCountBySQL(sql, parameters, null);
	}
	
	public static int queryCountBySQL(String sql, Map<String, Object> parameters) {
		return queryCountBySQL(sql, parameters, null);
	}
	
	/**
	 * 基于SQL查询数据条数。
	 * 
	 * @param sql
	 * @param parameters
	 * @param sessionFactoryName
	 * @return
	 */
	public static int queryCountBySQL(String sql, Object parameters, String sessionFactoryName) {
		return ((Number)queryUniqueResultBySQL(sql, parameters, sessionFactoryName)).intValue();
	}
	
	public static int queryCount(String hql) {
		return queryCount(hql, null, null);
	}
	
	public static int queryCount(String hql, String sessionFactoryName) {
		return queryCount(hql, null, sessionFactoryName);
	}

	public static int queryCount(String hql, Object[] parameters) {
		return queryCount(hql, parameters, null);
	}
	
	public static int queryCount(String hql, Map<String, Object> parameters) {
		return queryCount(hql, parameters, null);
	}
	
	/**
	 * 基于HQL查询数据条数。
	 * 
	 * @param hql
	 * @param parameters
	 * @param sessionFactoryName
	 * @return
	 */
	public static int queryCount(String hql, Object parameters, String sessionFactoryName) {
		return ((Number)queryUniqueResult(hql, parameters, sessionFactoryName)).intValue();
	}
	
	public static int queryCount(Query query) {
		return queryCount(query, null, null);
	}
	
	public static int queryCount(Query query, String sessionFactoryName) {
		return queryCount(query, null, sessionFactoryName);
	}

	public static int queryCount(Query query, Object[] parameters) {
		return queryCount(query, parameters, null);
	}
	
	public static int queryCount(Query query, Map<String, Object> parameters) {
		return queryCount(query, parameters, null);
	}
	
	/**
	 * 基于Query查询数据条数。
	 * 
	 * @param query
	 * @param parameters
	 * @param sessionFactoryName
	 * @return
	 */
	public static int queryCount(Query query, Object parameters, String sessionFactoryName) {
		return ((Number)queryUniqueResult(query, parameters, sessionFactoryName)).intValue();
	}
	
	public static <T> T queryUniqueResult(DetachedCriteria detachedCriteria) {
		return queryUniqueResult(detachedCriteria, null);
	}
	
	/**
	 * 基于QBC查询唯一结果。
	 * 
	 * @param detachedCriteria
	 * @param sessionFactoryName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T queryUniqueResult(DetachedCriteria detachedCriteria, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName);
		try {
			return (T) detachedCriteria.getExecutableCriteria(session).uniqueResult();
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}
	
	public static <T> T queryUniqueResult(String hql) {
		return queryUniqueResult(hql, null, null);
	}
	
	public static <T> T queryUniqueResult(String hql, String sessionFactoryName) {
		return queryUniqueResult(hql, null, sessionFactoryName);
	}
	
	public static <T> T queryUniqueResult(String hql, Object[] parameters) {
		return queryUniqueResult(hql, parameters, null);
	}
	
	public static <T> T queryUniqueResult(String hql, Map<String, Object> parameters) {
		return queryUniqueResult(hql, parameters, null);
	}

	/**
	 * 基于HQL查询唯一结果。
	 * 
	 * @param hql
	 * @param parameters
	 * @param sessionFactoryName
	 * @return
	 */
	public static <T> T queryUniqueResult(String hql, Object parameters, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName);
		try {
			Query query = session.createQuery(hql);
			return queryUniqueResult(query, parameters, sessionFactoryName);
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}
	
	public static <T> T queryUniqueResultBySQL(String sql) {
		return queryUniqueResult(sql, null, null);
	}
	public static <T> T queryUniqueResultBySQL(String sql, String sessionFactoryName) {
		return queryUniqueResult(sql, null, sessionFactoryName);
	}
	
	public static <T> T queryUniqueResultBySQL(String sql, Object[] parameters) {
		return queryUniqueResult(sql, parameters, null);
	}
	
	public static <T> T queryUniqueResultBySQL(String sql, Map<String, Object> parameters) {
		return queryUniqueResult(sql, parameters, null);
	}

	/**
	 * 基于SQL查询唯一结果。
	 * 
	 * @param sql
	 * @param parameters
	 * @param sessionFactoryName
	 * @return
	 */
	public static <T> T queryUniqueResultBySQL(String sql, Object parameters, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName);
		try {
			Query query = session.createSQLQuery(sql);
			return queryUniqueResult(query, parameters, sessionFactoryName);
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}

	public static <T> T queryUniqueResult(Query query) {
		return queryUniqueResult(query, null, null);
	}
	
	public static <T> T queryUniqueResult(Query query, String sessionFactoryName) {
		return queryUniqueResult(query, null, sessionFactoryName);
	}
	
	public static <T> T queryUniqueResult(Query query, Object[] parameters) {
		return queryUniqueResult(query, parameters, null);
	}
	
	public static <T> T queryUniqueResult(Query query, Map<String, Object> parameters) {
		return queryUniqueResult(query, parameters, null);
	}
	
	/**
	 * 基于Query查询唯一结果。
	 * 
	 * @param query
	 * @param parameters
	 * @param sessionFactoryName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T queryUniqueResult(Query query, Object parameters, String sessionFactoryName) {
		if (parameters instanceof Object[]) {
			setQueryParameters(query, (Object[]) parameters);
		} else {
			setQueryParameters(query, (Map<String, Object>) parameters);
		}
		return (T) query.uniqueResult();
	}
	
	public static int executeUpdate(String hql) {
		return executeUpdate(hql, null, null);
	}
	
	public static int executeUpdate(String hql, String sessionFactoryName) {
		return executeUpdate(hql, null, sessionFactoryName);
	}
	
	public static int executeUpdate(String hql, Object[] parameters) {
		return executeUpdate(hql, parameters, null);
	}
	
	public static int executeUpdate(String hql, Map<String, Object> parameters) {
		return executeUpdate(hql, parameters, null);
	}
	
	/**
	 * 基于HQL执行更新。
	 * 
	 * @param hql
	 * @param parameters
	 * @param sessionFactoryName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static int executeUpdate(String hql, Object parameters, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName);
		try {
			Query query = session.createQuery(hql);
			if (parameters instanceof Object[]) {
				setQueryParameters(query, (Object[]) parameters);
			} else {
				setQueryParameters(query, (Map<String, Object>) parameters);
			}
			return query.executeUpdate();
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}
	}
	
	public static int executeUpdateBySQL(String sql) {
		return executeUpdateBySQL(sql, null, null);
	}
	
	public static int executeUpdateBySQL(String sql, String sessionFactoryName) {
		return executeUpdateBySQL(sql, null, sessionFactoryName);
	}
	
	public static int executeUpdateBySQL(String sql, Object[] parameters) {
		return executeUpdateBySQL(sql, parameters, null);
	}
	
	public static int executeUpdateBySQL(String sql, Map<String, Object> parameters) {
		return executeUpdateBySQL(sql, parameters, null);
	}
	
	/**
	 * 基于SQL执行更新。
	 * 
	 * @param sql
	 * @param parameters
	 * @param sessionFactoryName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static int executeUpdateBySQL(String sql, Object parameters, String sessionFactoryName) {
		Session session = getSession(sessionFactoryName);
		try {
			Query query = session.createSQLQuery(sql);
			if (parameters instanceof Object[]) {
				setQueryParameters(query, (Object[]) parameters);
			} else {
				setQueryParameters(query, (Map<String, Object>) parameters);
			}
			return query.executeUpdate();
		} finally {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				session.flush();
				session.close();
			}
		}	
	}
	
	
	public static void setQueryParameters(Query query, Object[] parameters) {
		if (parameters != null) {
			for (int i = 0; i < parameters.length; i++) {
				query.setParameter(i, parameters[i]);
			}
		}
	}

	public static void setQueryParameters(Query query, Map<String, Object> parameters) {
		if (parameters != null) {
			for (String name : parameters.keySet()) {
				Object obj = parameters.get(name);
				if (obj == null) {
					continue;
				} else if (obj instanceof Collection) {
					query.setParameterList(name, (Collection<?>) obj);
				} else if (obj instanceof Object[]) {
					query.setParameterList(name, (Object[]) obj);
				} else {
					query.setParameter(name, obj);
				}
			}
		}
	}
	
	public static SessionFactory getSessionFactory(String sessionFactoryName, Class<?> entityClass) {
		if (StringUtils.isNotEmpty(sessionFactoryName)) {
			return getSessionFactory(sessionFactoryName);
		} else if (entityClass != null) {
			SessionFactory sf = getSessionFactory(entityClass);
			if (sf != null) {
				return sf;
			}
		}
		return defaultSessionFactory;
	}
	
	public static String getSessionFactoryName(String sessionFactoryName, Class<?> entityClass) {
		if (StringUtils.isNotEmpty(sessionFactoryName)) {
			return sessionFactoryName;
		} else if (entityClass != null) {
			for (Entry<String, SessionFactory> entry : sessionFactoryMap.entrySet()) {
				if(isEntityClass(entry.getValue(), entityClass)) {
					return entry.getKey();
				}
			}
		}
		return null;
	}
	
	public static SessionFactory getSessionFactory() {
		return defaultSessionFactory;
	}
	
	public static SessionFactory getSessionFactory(String sessionFactoryName) {
		if (StringUtils.isEmpty(sessionFactoryName)) {
			return defaultSessionFactory;
		}
		return sessionFactoryMap.get(sessionFactoryName);
	}
	
	private static Session getSession(String sessionFactoryName) {
		return getSession(sessionFactoryName, null);
	}
	
	private static Session getSession(String sessionFactoryName, Class<?> entityClass) {
		boolean active = TransactionSynchronizationManager.isSynchronizationActive();
		if (active) {
			return getSessionFactory(sessionFactoryName, entityClass).getCurrentSession();
		} 
		return getSessionFactory(sessionFactoryName, entityClass).openSession();
	}
	
	
	public static Session getCurrentSession() {
		return getCurrentSession(null);
	}
	
	public static Session getCurrentSession(String sessionFactoryName) {
		if (StringUtils.isEmpty(sessionFactoryName)) {
			return defaultSessionFactory.getCurrentSession();
		}
		return getSessionFactory(sessionFactoryName).getCurrentSession();
	}
	
	public static SessionFactory getSessionFactory(Class<?> entityClass) {
		for (SessionFactory sessionFactory : sessionFactoryMap.values()) {
			if(isEntityClass(sessionFactory, entityClass)) {
				return sessionFactory;
			}
		}
		return null;
	}
	
	public static Map<String, SessionFactory> getSessionFactoryMap() {	
		return sessionFactoryMap;
	}
	
	public static boolean isEntityClass(Class<?> entityClass) {
		for (SessionFactory sessionFactory : sessionFactoryMap.values()) {
			if(isEntityClass(sessionFactory, entityClass)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isEntityClass(SessionFactory sessionFactory, Class<?> entityClass) {
		return sessionFactory.getClassMetadata(entityClass) != null;
	}
	
	public static PersistentClass getPersistentClass(Class<?> entityClass) {
		Configuration configuration = null;
		if (entityClass !=null) {
			for (Entry<String, SessionFactory> entry : sessionFactoryMap.entrySet()) {
				if(isEntityClass(entry.getValue(), entityClass)) {
					Object bean = applicationContext.getBean("&" + entry.getKey());
					if (bean instanceof LocalSessionFactoryBean) {
						LocalSessionFactoryBean b = (LocalSessionFactoryBean) bean;
						configuration = b.getConfiguration();
					} else if (bean instanceof org.springframework.orm.hibernate4.LocalSessionFactoryBean) {
						org.springframework.orm.hibernate4.LocalSessionFactoryBean b = (org.springframework.orm.hibernate4.LocalSessionFactoryBean) bean;
						configuration = b.getConfiguration();
					}
					break;
				}
			}
			if (configuration !=null) {
				return configuration.getClassMapping(entityClass.getName());
			}
		}
        return null;  
	}
	
	public static String getTableName(Class<?> entityClass) {
		PersistentClass persistentClass = getPersistentClass(entityClass);
		if (persistentClass !=null) {
			return persistentClass.getTable().getName();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static String getColumnName(Class<?> entityClass, String propertyName) {
		PersistentClass persistentClass = getPersistentClass(entityClass);
		if (persistentClass !=null) {
			Property property = persistentClass.getProperty(propertyName);
			Iterator<Column> iterator = property.getColumnIterator();
			if (iterator.hasNext()) {
				Column column = iterator.next();
				return column.getName();
			}
		}
		return null;
	}
	
	public static String getPKColumnName(Class<?> entityClass) {
		PersistentClass persistentClass = getPersistentClass(entityClass);
		if (persistentClass !=null) {
			return getPrimaryKey(entityClass).getColumn(0).getName();
		}
		return null;
	}
	
	public static PrimaryKey getPrimaryKey(Class<?> entityClass) {
		PersistentClass persistentClass = getPersistentClass(entityClass);
		if (persistentClass !=null) {
			return persistentClass.getTable().getPrimaryKey();
		}
		return null;
	}
	
	public static Property getIdProperty(Class<?> entityClass) {
		PersistentClass persistentClass = getPersistentClass(entityClass);
		if (persistentClass !=null) {
			return persistentClass.getIdentifierProperty();
		}
		return null;
	}
	
	public static String getIdPropertyName(Class<?> entityClass) {
		PersistentClass persistentClass = getPersistentClass(entityClass);
		if (persistentClass !=null) {
			return persistentClass.getIdentifierProperty().getName();
		}
		return null;
	}



	@SuppressWarnings("unchecked")
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		HibernateUtils.applicationContext = applicationContext;
		
		HibernateUtils.sessionFactoryMap = applicationContext.getBeansOfType(SessionFactory.class);
		HibernateUtils.defaultSavePolicy = 
				(SavePolicy) applicationContext.getBean(Configure.getString(Constants.DEFAULT_SAVE_POLICY_PROP));
		HibernateUtils.defaultQBCCriteriaPolicy = 
				(CriteriaPolicy<DetachedCriteria>) applicationContext.getBean(Configure.getString(Constants.DEFAULT_QBC_CRITERIA_POLICY_PROP));
		HibernateUtils.defaultSQLCriteriaPolicy = 
				(CriteriaPolicy<AfterWhere>) applicationContext.getBean(Configure.getString(Constants.DEFAULT_SQL_CRITERIA_POLICY_PROP));
		HibernateUtils.defaultHQLCriteriaPolicy = 
				(CriteriaPolicy<AfterWhere>) applicationContext.getBean(Configure.getString(Constants.DEFAULT_HQL_CRITERIA_POLICY_PROP));
		if (HibernateUtils.sessionFactoryMap.size() == 1) {
			HibernateUtils.defaultSessionFactory = HibernateUtils.sessionFactoryMap.values().iterator().next();
		} else {
			String defaultSessionFactoryProp = Configure.getString(Constants.DEFAULT_SESSION_FACTORY_PROP);
			Assert.hasText(defaultSessionFactoryProp, Constants.DEFAULT_SESSION_FACTORY_PROP + " can not be empty!");
			HibernateUtils.defaultSessionFactory = 
					(SessionFactory) applicationContext.getBean(Configure.getString(Constants.DEFAULT_SESSION_FACTORY_PROP));
		}

		
		
	}
	
}
