 package com.bstek.dorado.dao.hibernate.query.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.sf.cglib.beans.BeanMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.bstek.dorado.dao.hibernate.HibernateUtils;
import com.bstek.dorado.dao.hibernate.parser.CriterionParser;
import com.bstek.dorado.dao.hibernate.parser.SmartSubQueryParser;
import com.bstek.dorado.dao.hibernate.parser.SubQueryParser;
import com.bstek.dorado.dao.hibernate.policy.LinqContext;
import com.bstek.dorado.dao.hibernate.policy.impl.QBCCriteriaContext;
import com.bstek.dorado.dao.hibernate.query.CollectInfo;
import com.bstek.dorado.dao.hibernate.query.Filter;
import com.bstek.dorado.dao.hibernate.query.Linq;
import com.bstek.dorado.data.entity.EntityUtils;
import com.bstek.dorado.data.provider.Criteria;
import com.bstek.dorado.data.provider.Page;

/**
 *@author Kevin.yang
 *@since 2015年6月10日
 */
public class LinqImpl implements Linq {
	private DetachedCriteria detachedCriteria;
	private Criteria criteria;
	private Stack<Junction> junctions = new Stack<Junction>();
	private Page<?> page;
	private boolean toEntity;
	private Class<?> entityClass;
	private String sessionFactoryName;
	private List<CollectInfo> collectInfos = new ArrayList<CollectInfo>();
	private Map<Class<?>, String[]> projectionMap = new HashMap<Class<?>, String[]>();
	private LinqContext linqContext = new LinqContext();
	private List<CriterionParser<Criterion>> criterionParsers = new ArrayList<CriterionParser<Criterion>>();

	private Filter filter;
	private boolean disableSmartSubQueryCriterion;
	private String alias;
	private boolean disableBackFillFilter; 
	private int maxInParam = 800;
	
	private Linq parent;
	
	private LinqImpl(Class<?> entityClass){
		this(entityClass, "__alia__");
	}
	
	private LinqImpl(Class<?> entityClass, String alias){
		this.entityClass = entityClass;
		this.alias = alias;
		if (StringUtils.isEmpty(alias)) {
			detachedCriteria = DetachedCriteria.forClass(entityClass);
		} else {
			detachedCriteria = DetachedCriteria.forClass(entityClass, alias);
		}
	}
	
	public final static LinqImpl forClass(Class<?> entityClass) {
		return new LinqImpl(entityClass);
	}
	
	public final static Linq forClass(Class<?> entityClass, String alias) {
		return new LinqImpl(entityClass, alias);
	}

 	@Override
	public Linq select(String... properties) {
 		ProjectionList projectionList = Projections.projectionList();
		for (String property : properties) {
			String[] ps = property.split("\\s*,\\s*");
			for (String p : ps) {
				String alias = StringUtils.trim(p);
				String[] pa = alias.split("\\s+[aA][sS]\\s+");
				if (pa.length > 1) {
					alias = pa[1];
				} else {
					pa = alias.split("\\s+");
					if (pa.length > 1) {
						alias = pa[1];
					}
				}
				projectionList.add(Projections.property(pa[0]).as(alias));
			}
		}
		detachedCriteria.setProjection(projectionList);
		if (properties.length > 1) {
			aliasToBean();
		}
		return this;
	}

 	@Override
	public Linq select(Projection... projections) {
 		ProjectionList projectionList = Projections.projectionList();
		for (Projection projection : projections) {
			projectionList.add(projection);
		}
		detachedCriteria.setProjection(projectionList);
		if (projections.length > 1) {
			aliasToBean();
		}
		return this;
	}
	
	@Override
	public Linq add(Criterion... criterions) {
		for (Criterion criterion : criterions) {
			if (junctions.empty()) {
				detachedCriteria.add(criterion);
			} else {
				junctions.peek().add(criterion);
			}
		}
		return this;
	}
	
	@Override
	public Linq addIf(boolean need, Criterion... criterions) {
		if (need) {
			add(criterions);
		}
		return this;
	}
	
	@Override
	public Linq addIfNotNull(Object target, Criterion... criterions) {
		if (target == null || (target instanceof String && StringUtils.isEmpty(target.toString()))) {
			return this;
		}
		add(criterions);
		return this;
	}
	
	@Override
	public Linq propertyEq(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyEq(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyEq(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyEqAll(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyEqAll(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyEqAll(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyGe(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyGe(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyGe(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyGeAll(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyGeAll(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyGeAll(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyGeSome(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyGeSome(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyGeSome(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyGt(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyGt(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyGt(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyGtAll(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyGtAll(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyGtAll(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyGtSome(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.gtSome(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.gtSome(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyLt(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyLt(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyLt(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyLtAll(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyLtAll(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyLtAll(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyLtSome(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyLtSome(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyLtSome(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyLe(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyLe(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyLe(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyLeAll(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyLeAll(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyLeAll(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyLeSome(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyLeSome(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyLeSome(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyIn(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyIn(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyIn(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq propertyNotIn(String property, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.propertyNotIn(property, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.propertyNotIn(property, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq exists(Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		linq.select(Projections.id());
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.exists(linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.exists(linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq notExists(Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		linq.select(Projections.id());
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.notExists(linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.notExists(linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq eq(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.eq(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.eq(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq eqAll(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.eqAll(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.eqAll(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq ge(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.ge(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.ge(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq geAll(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.geAll(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.geAll(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq geSome(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.geSome(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.geSome(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq gt(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.gt(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.gt(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq gtAll(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.gtAll(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.gtAll(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq gtSome(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.gtSome(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.gtSome(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq lt(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.lt(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.lt(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq ltAll(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.ltAll(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.ltAll(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq ltSome(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.ltSome(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.ltSome(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq le(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.le(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.le(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq leAll(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.leAll(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.leAll(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq leSome(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.leSome(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.leSome(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq in(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.in(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.in(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	@Override
	public Linq notIn(Object value, Class<?> entityClass, String alias) {
		Linq linq = HibernateUtils.createLinq(entityClass, alias);
		linq.setParent(this);
		if (junctions.empty()) {
			detachedCriteria.add(Subqueries.notIn(value, linq.detachedCriteria()));
		} else {
			junctions.peek().add(Subqueries.notIn(value, linq.detachedCriteria()));
		}
		return linq;
	}
	
	
	
	
	
	@Override
	public Linq back() {
		if (junctions.empty()) {
			return parent;
		} else {
			junctions.pop();
			return this;
		}
	}
	
	@Override
	public Linq and(Criterion lhs, Criterion rhs) {
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.and(lhs, rhs));
		} else {
			junctions.peek().add(Restrictions.and(lhs, rhs));
		}
		return this;
	}
	
	@Override
	public Linq and() {
		Junction junction = Restrictions.conjunction();
		if (junctions.empty()) {
			detachedCriteria.add(junction);
		} else {
			junctions.peek().add(junction);
		}
		junctions.push(junction);
		return this;
	}
	
	@Override
	public Linq or(Criterion lhs, Criterion rhs) {
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.or(lhs, rhs));
		} else {
			junctions.peek().add(Restrictions.or(lhs, rhs));
		}
		return this;
	}
	
	@Override
	public Linq or() {
		Junction junction = Restrictions.disjunction();
		if (junctions.empty()) {
			detachedCriteria.add(junction);
		} else {
			junctions.peek().add(junction);
		}
		junctions.push(junction);
		return this;
	}
	
	@Override
	public Linq not(Criterion expression) {
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.not(expression));
		} else {
			junctions.peek().add(Restrictions.not(expression));
		}
		return this;
	}
	
	@Override
	public Linq eq(String property, Object value) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.eq(property, value));
		} else {
			junctions.peek().add(Restrictions.eq(property, value));
		}
		return this;
	}
	
	@Override
	public Linq eqProperty(String property, String otherProperty) {
		Assert.hasText(property, "property must has text");
		Assert.hasText(property, "otherProperty must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.eqProperty(property, otherProperty));
		} else {
			junctions.peek().add(Restrictions.eqProperty(property, otherProperty));
		}
		return this;
	}
	
	@Override
	public Linq ne(String property, Object value) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.ne(property, value));
		} else {
			junctions.peek().add(Restrictions.ne(property, value));
		}
		return this;
	}
	
	@Override
	public Linq neProperty(String property, String otherProperty) {
		Assert.hasText(property, "property must has text");
		Assert.hasText(property, "otherProperty must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.neProperty(property, otherProperty));
		} else {
			junctions.peek().add(Restrictions.neProperty(property, otherProperty));
		}
		return this;
	}
	
	@Override
	public Linq ge(String property, Object value) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.ge(property, value));
		} else {
			junctions.peek().add(Restrictions.ge(property, value));
		}
		return this;
	}
	
	@Override
	public Linq geProperty(String property, String otherProperty) {
		Assert.hasText(property, "property must has text");
		Assert.hasText(property, "otherProperty must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.geProperty(property, otherProperty));
		} else {
			junctions.peek().add(Restrictions.geProperty(property, otherProperty));
		}
		return this;
	}
	
	@Override
	public Linq gt(String property, Object value) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.gt(property, value));
		} else {
			junctions.peek().add(Restrictions.gt(property, value));
		}
		return this;
	}
	
	@Override
	public Linq gtProperty(String property, String otherProperty) {
		Assert.hasText(property, "property must has text");
		Assert.hasText(property, "otherProperty must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.gtProperty(property, otherProperty));
		} else {
			junctions.peek().add(Restrictions.gtProperty(property, otherProperty));
		}
		return this;
	}
	
	@Override
	public Linq le(String property, Object value) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.le(property, value));
		} else {
			junctions.peek().add(Restrictions.le(property, value));
		}
		return this;
	}
	
	@Override
	public Linq leProperty(String property, String otherProperty) {
		Assert.hasText(property, "property must has text");
		Assert.hasText(property, "otherProperty must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.leProperty(property, otherProperty));
		} else {
			junctions.peek().add(Restrictions.leProperty(property, otherProperty));
		}
		return this;
	}
	
	@Override
	public Linq lt(String property, Object value) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.lt(property, value));
		} else {
			junctions.peek().add(Restrictions.lt(property, value));
		}
		return this;
	}
	
	@Override
	public Linq ltProperty(String property, String otherProperty) {
		Assert.hasText(property, "property must has text");
		Assert.hasText(property, "otherProperty must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.ltProperty(property, otherProperty));
		} else {
			junctions.peek().add(Restrictions.ltProperty(property, otherProperty));
		}
		return this;
	}
	
	@Override
	public Linq between(String property, Object lo, Object hi) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.between(property, lo, hi));
		} else {
			junctions.peek().add(Restrictions.between(property, lo, hi));
		}
		return this;
	}
	
	@Override
	public Linq idEq(Object value) {
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.idEq(value));
		} else {
			junctions.peek().add(Restrictions.idEq(value));
		}
		return this;
	}
	
	@Override
	public Linq in(String property, Collection<?> values) {
		Assert.hasText(property, "property must has text.");
		Assert.notEmpty(values, "values must not be empty.");
		in(property, values.toArray());
		return this;
	}
	
	private List<Object[]> splitArray(Object[] array) {
		List<Object[]> result = new ArrayList<Object[]>();
		int block = array.length/maxInParam;
		int i = 0;
		for (i = 0; i < block; i++) {
			result.add(Arrays.copyOfRange(array, i*maxInParam, i*maxInParam + maxInParam));
		}
		if (array.length%maxInParam != 0) {
			result.add(Arrays.copyOfRange(array, i*maxInParam, array.length));
		}
		return result;
	}
	
	@Override
	public Linq in(String property, Object... values) {
		Assert.hasText(property, "property must has text.");
		Assert.notEmpty(values, "values must not be empty.");
		if (values.length > maxInParam) {
			List<Object[]> list = splitArray(values);
			Disjunction disjunction = Restrictions.disjunction();
			for (Object[] objects : list) {
				disjunction.add(Restrictions.in(property, objects));
			}
			if (junctions.empty()) {
				detachedCriteria.add(disjunction);
			} else {
				junctions.peek().add(disjunction);
			}
		} else {
			if (junctions.empty()) {
				detachedCriteria.add(Restrictions.in(property, values));
			} else {
				junctions.peek().add(Restrictions.in(property, values));
			}
		}
		return this;
	}
	
	@Override
	public Linq like(String property, Object value) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.like(property, value));
		} else {
			junctions.peek().add(Restrictions.like(property, value));
		}
		return this;
	}
	
	@Override
	public Linq like(String property, String value, MatchMode matchMode) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.like(property, value, matchMode));
		} else {
			junctions.peek().add(Restrictions.like(property, value, matchMode));
		}
		return this;
	}
	
	@Override
	public Linq ilike(String property, Object value) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.ilike(property, value));
		} else {
			junctions.peek().add(Restrictions.ilike(property, value));
		}
		return this;
	}
	
	@Override
	public Linq ilike(String property, String value, MatchMode matchMode) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.ilike(property, value, matchMode));
		} else {
			junctions.peek().add(Restrictions.ilike(property, value, matchMode));
		}
		return this;
	}
	
	@Override
	public Linq isNull(String property) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.isNull(property));
		} else {
			junctions.peek().add(Restrictions.isNull(property));
		}
		return this;
	}
	
	@Override
	public Linq isNotNull(String property) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.isNotNull(property));
		} else {
			junctions.peek().add(Restrictions.isNotNull(property));
		}
		return this;
	}
	
	@Override
	public Linq isEmpty(String property) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.isEmpty(property));
		} else {
			junctions.peek().add(Restrictions.isEmpty(property));
		}
		return this;
	}
	
	@Override
	public Linq isNotEmpty(String property) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.isNotEmpty(property));
		} else {
			junctions.peek().add(Restrictions.isNotEmpty(property));
		}
		return this;
	}
	
	@Override
	public Linq sizeEq(String property, int size) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.sizeEq(property, size));
		} else {
			junctions.peek().add(Restrictions.sizeEq(property, size));
		}
		return this;
	}
	
	@Override
	public Linq sizeGe(String property, int size) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.sizeGe(property, size));
		} else {
			junctions.peek().add(Restrictions.sizeGe(property, size));
		}
		return this;
	}
	
	@Override
	public Linq sizeGt(String property, int size) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.sizeGt(property, size));
		} else {
			junctions.peek().add(Restrictions.sizeGt(property, size));
		}
		return this;
	}
	
	@Override
	public Linq sizeLe(String property, int size) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.sizeLe(property, size));
		} else {
			junctions.peek().add(Restrictions.sizeLe(property, size));
		}
		return this;
	}
	
	@Override
	public Linq sizeLt(String property, int size) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.sizeLt(property, size));
		} else {
			junctions.peek().add(Restrictions.sizeLt(property, size));
		}
		return this;
	}
	
	@Override
	public Linq sizeNe(String property, int size) {
		Assert.hasText(property, "property must has text");
		if (junctions.empty()) {
			detachedCriteria.add(Restrictions.sizeNe(property, size));
		} else {
			junctions.peek().add(Restrictions.sizeNe(property, size));
		}
		return this;
	}
	
	
	@Override
	public Linq where(Criteria criteria) {
		where(criteria, new Criterion[]{});
		return this;
	}
	
	@Override
	public Linq where(Criterion... criterions) {
		this.add(criterions);
		return this;
	}
	
	@Override
	public Linq where(Criteria criteria, Criterion... criterions) {
		this.criteria = criteria;
		this.add(criterions);
		return this;
	}

	@Override
	public Linq orders(Order... orders) {
		for (Order order : orders) {
			detachedCriteria.addOrder(order);
		}
		return this;
	}

	@Override
	public Linq aliasToBean() {
		setTransformer(Transformers.aliasToBean(entityClass));
		return this;
	}

	@Override
	public Linq aliasToBean(Class<?> cls) {
		setTransformer(Transformers.aliasToBean(cls));
		return this;
	}

	@Override
	public Linq setTransformer(ResultTransformer transformer) {
		detachedCriteria.setResultTransformer(transformer);
		return this;
	}

	@Override
	public Linq setPage(Page<?> page) {
		this.page = page;
		return this;
	}

	@Override
	public Linq setSessionFactoryName(String sessionFactoryName) {
		this.sessionFactoryName = sessionFactoryName;
		return this;
	}

	@Override
	public DetachedCriteria detachedCriteria() {
		return detachedCriteria;
	}
	
	@Override
	public Linq filter(Filter filter) {
		this.filter = filter;
		return this;
	}

	@Override
	public Page<?> paging() {
		if (parent != null) {
			if (page != null) {
				return parent.paging(page);
			}
			return parent.paging();
		}
		Assert.notNull(page, "page can not be null");
		beforeExecute();
		HibernateUtils.pagingQuery(page, detachedCriteria, HibernateUtils.getSessionFactoryName(sessionFactoryName, entityClass));
		afterExecute(page.getEntities());
		return page;
	}
	
	protected void beforeExecute() {
		if (!disableSmartSubQueryCriterion) {
			this.addParser(new SmartSubQueryParser(entityClass, collectInfos, alias));
		}
		doParseCriteria();
	}
	
	protected void afterExecute(Collection<?> entities) {
		doCollect(entities);
		doFilter(entities);
	}
	
	protected void doParseCriteria() {
		if (criteria != null) {
			QBCCriteriaContext context = new QBCCriteriaContext();
			context.setCriteria(criteria);
			context.setEntityClass(entityClass);
			context.setDetachedCriteria(detachedCriteria);
			context.setCriterionParsers(criterionParsers);
			HibernateUtils.getDefaultQBCCriteriaPolicy().apply(context);
		}
	}
	
	@Override
	public Linq setDisableSmartSubQueryCriterion() {
		this.disableSmartSubQueryCriterion = true;
		return this;
	}
	
	@Override
	public Linq setDisableBackFillFilter() {
		this.disableBackFillFilter = true;
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

	@Override
	public <T> List<T> list() {
		if (parent != null) {
			return parent.list();
		}
		beforeExecute();
		List<T> list = HibernateUtils.query(detachedCriteria, HibernateUtils.getSessionFactoryName(sessionFactoryName, entityClass));
		afterExecute(list);
		return list;
	}

	@Override
	public <T> T uniqueResult() {
		if (parent != null) {
			return parent.uniqueResult();
		}
		beforeExecute();
		T t = HibernateUtils.queryUniqueResult(detachedCriteria, HibernateUtils.getSessionFactoryName(sessionFactoryName, entityClass));	
		if (t != null) {
			List<T> list = new ArrayList<T>();
			list.add(t);
			afterExecute(list);
			return list.size() > 0 ? list.get(0) : null;
		}
		return null;
	}

	@Override
	public Linq toEntity() {
		this.toEntity = true;
		return this;
	}

	@Override
	public Linq addParser(CriterionParser<Criterion> criterionParser) {
		this.criterionParsers.add(criterionParser);
		return this;
	}
	
	@Override
	public Linq addSubQueryParser(Class<?>... entityClasses) {
		for (Class<?> entityClass : entityClasses) {
			this.addParser(new SubQueryParser(entityClass, alias));
		}
		return this;
	}
	
	@Override
	public Linq addSubQueryParser(Class<?> entityClass, String... foreignKeys) {
		this.addParser(new SubQueryParser(entityClass, alias, foreignKeys));
		return this;
	}

	@Override
	public LinqContext getLinqContext() {
		return linqContext;
	}
	
	@Override
	public Linq collect(String ...properties) {
		collect(null, properties);
		return this;
	}
	
	@Override
	public Linq collect(Class<?> entityClass, String ...properties) {
		CollectInfo collectInfo = new CollectInfo();
		collectInfo.setEntityClass(entityClass);
		collectInfo.setProperties(properties);
		collectInfos.add(collectInfo);
		return this;
	}
	
	
	@Override
	public Linq collectSelect(Class<?> entityClass, String ...projections) {
		projectionMap.put(entityClass, projections);
		return this;
	}
	
	@SuppressWarnings("rawtypes")
	protected void doCollect(Collection list) {
		if (!collectInfos.isEmpty()) {
			initCollectInfos(list);
			buildMetadata();
		}
		doBackfill();
	}

	@SuppressWarnings("rawtypes")
	private void buildMetadata() {
		Map<Object, Object> metadata = linqContext.getMetadata();
		for (CollectInfo collectInfo : collectInfos) {
			Class<?> entityClass = collectInfo.getEntityClass();
			String idProperty = HibernateUtils.getIdPropertyName(entityClass);
			if (!CollectionUtils.isEmpty(collectInfo.getList())) {
				for (String property : collectInfo.getProperties()) {
					
					if (!metadata.containsKey(property)) {
						if (collectInfo.getEntityClass() != null) {
							if (metadata.containsKey(entityClass)) {
								metadata.put(property, metadata.get(entityClass));
							} else {
								Linq linq = HibernateUtils.createLinq(entityClass);
								linq.in(idProperty, collectInfo.getList());
								if (ArrayUtils.isNotEmpty(projectionMap.get(entityClass))) {
									linq.select(projectionMap.get(entityClass));
									linq.aliasToBean();
								}
								List result = linq.list();
								Map<Object, Object> map = new HashMap<Object, Object>();
								for (Object obj : result) {
									BeanMap beanMap = BeanMap.create(obj);
									map.put(beanMap.get(idProperty), obj);
								}
								metadata.put(property, map);
								metadata.put(entityClass, map);
							}
							
						} else {
							metadata.put(property, collectInfo.getList());
						}
					}
				}
				
			}
			
		}
	}

	private void initCollectInfos(Collection<?> list) {
		for (Object entity : list) {
			BeanMap beanMap = BeanMap.create(entity);
			for (CollectInfo collectInfo : collectInfos) {
				for (String property : collectInfo.getProperties()) {
					Object value = beanMap.get(property);
					if (value != null) {
						collectInfo.add(value);
					}
				}
			}
		}
	}
	
	private void doBackfill() {
		if (!collectInfos.isEmpty() && !disableBackFillFilter) {
			this.filter = new BackfillFilter(this.filter, collectInfos);
		}
		
	}

	@Override
	public Page<?> paging(Page<?> page) {
		this.setPage(page);
		return paging();
	}

	@Override
	public Linq getParent() {
		return parent;
	}

	@Override
	public Linq setParent(Linq parent) {
		this.parent = parent;
		return this;
	}


}
