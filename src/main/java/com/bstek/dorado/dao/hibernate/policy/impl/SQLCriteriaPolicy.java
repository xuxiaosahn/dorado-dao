package com.bstek.dorado.dao.hibernate.policy.impl;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bstek.dorado.dao.AfterWhere;
import com.bstek.dorado.dao.CriteriaUtils;
import com.bstek.dorado.dao.hibernate.HibernateUtils;
import com.bstek.dorado.dao.hibernate.parser.CriterionParser;
import com.bstek.dorado.dao.hibernate.policy.CriteriaContext;
import com.bstek.dorado.data.provider.Order;
import com.bstek.dorado.data.provider.filter.SingleValueFilterCriterion;

/**
 *@author Kevin.yang
 *@since 2015年5月23日
 */
public class SQLCriteriaPolicy extends HQLCriteriaPolicy {

	@Override
	protected void parseOrder(CriteriaContext context) {
		QLCriteriaContext c = (QLCriteriaContext) context;
		String alias = c.getAlias();
		Order order = c.getCurrent();
		if (StringUtils.isEmpty(alias)) {
			c.getOrders().add(getColumnName(c.getEntityClass(), order.getProperty()) + (order.isDesc() ? " desc" : ""));
		} else {
			c.getOrders().add(alias + "." + getColumnName(c.getEntityClass(), order.getProperty()) + (order.isDesc() ? " desc" : ""));
		}
		
	}

	@Override
	protected void parseSingleValueFilterCriterion(CriteriaContext context) {
		QLCriteriaContext criteriaContext = (QLCriteriaContext) context;
		SingleValueFilterCriterion criterion = criteriaContext.getCurrent();
		List<CriterionParser<StringBuilder>> criterionParsers = criteriaContext.getCriterionParsers();
		String property = criterion.getProperty();
		String alias = criteriaContext.getAlias();
		Class<?> cls = criteriaContext.getEntityClass();
		AfterWhere afterWhere = criteriaContext.getAfterWhere();
		property = getColumnName(cls, property);
		if (StringUtils.isEmpty(alias)) {
			alias = StringUtils.EMPTY;
		} else {
			alias += ".";
		}
		criterion.setProperty(alias + property);
		
		StringBuilder c = null;
		for (CriterionParser<StringBuilder> criterionParser : criterionParsers) {
			c = criterionParser.parse(criterion);
			if (c != null) {
				break;
			}
		}
		if (c == null) {
			c = CriteriaUtils.parseQL(criterion);
		}
		afterWhere.getParamMap().put(criterion.getProperty(), criterion.getValue());
		criteriaContext.getCriterions().add(c.toString());
	}
	
	protected String getColumnName(Class<?> entityClass, String propertyName) {
		if (entityClass !=null) {
			String columnName = HibernateUtils.getColumnName(entityClass, propertyName);
			if (columnName != null) {
				return columnName;
			}
		}
		return propertyName;
	}

}
