package com.bstek.dorado.dao.hibernate.policy.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bstek.dorado.dao.AfterWhere;
import com.bstek.dorado.dao.CriteriaUtils;
import com.bstek.dorado.dao.hibernate.parser.CriterionParser;
import com.bstek.dorado.dao.hibernate.policy.CriteriaContext;
import com.bstek.dorado.data.provider.And;
import com.bstek.dorado.data.provider.Criteria;
import com.bstek.dorado.data.provider.Junction;
import com.bstek.dorado.data.provider.Or;
import com.bstek.dorado.data.provider.Order;
import com.bstek.dorado.data.provider.filter.SingleValueFilterCriterion;

/**
 *@author Kevin.yang
 *@since 2015年5月23日
 */
public class HQLCriteriaPolicy extends AbstractCriteriaPolicy<AfterWhere> {

	
	@Override
	public AfterWhere apply(CriteriaContext context) {
		QLCriteriaContext c = (QLCriteriaContext) context;
		AfterWhere afterWhere = new AfterWhere();
		Criteria criteria = c.getCriteria();
		c.setCriteria(criteria);
		c.setAfterWhere(afterWhere);
		if (criteria != null) {
			if(criteria.getCriterions() != null && !criteria.getCriterions().isEmpty()) {
				c.setCurrent(criteria.getCriterions());
				afterWhere.getWherePart().append(" where ( ");
				parseCriterions(c);
				afterWhere.getWherePart().append(StringUtils.join(c.getCriterions(), " and "));
				afterWhere.getWherePart().append(" ) ");
			}
			List<Order> orders = criteria.getOrders();
			if(orders !=null && !orders.isEmpty()) {
				afterWhere.getOrderPart().append(" order by ");
				parseOrders(context);
				afterWhere.getOrderPart().append(StringUtils.join(c.getOrders(), " , "));
			}
		}
		
		if(StringUtils.isBlank(afterWhere.getWherePart().toString())) {
			afterWhere.getWherePart().append(AfterWhere.START_WHERE);
		}
		return afterWhere;
	}

	@Override
	protected void parseOrder(CriteriaContext context) {
		QLCriteriaContext c = (QLCriteriaContext) context;
		String alias = c.getAlias();
		Order order = c.getCurrent();
		if (StringUtils.isEmpty(alias)) {
			c.getOrders().add(order.getProperty() + (order.isDesc() ? " desc" : ""));
		} else {
			c.getOrders().add(alias + "." + order.getProperty() + (order.isDesc() ? " desc" : ""));
		}
		
	}

	@Override
	protected void parseAndCriterion(CriteriaContext context) {
		QLCriteriaContext c = (QLCriteriaContext) context;
		StringBuilder wherePart = c.getAfterWhere().getWherePart();
		Junction oldJ = c.getJunction();
		List<String> criterions = c.getCriterions();
		And and = c.getCurrent();

		c.setJunction(and);
		c.setCurrent(and.getCriterions());
		c.setCriterions(new ArrayList<String>());
		
		parseCriterions(c);
		wherePart.append(" ( ")
			.append(StringUtils.join(c.getCriterions(), " and "))
			.append(" ) ");
		
		c.setJunction(oldJ);
		c.setCriterions(criterions);
	}

	@Override
	protected void parseOrCriterion(CriteriaContext context) {
		QLCriteriaContext c = (QLCriteriaContext) context;
		StringBuilder wherePart = c.getAfterWhere().getWherePart();
		List<String> criterions = c.getCriterions();
		Junction oldJ = c.getJunction();
		Or or = c.getCurrent();
		c.setJunction(or);
		c.setCurrent(or.getCriterions());
		c.setCriterions(new ArrayList<String>());
		
		parseCriterions(c);
		wherePart.append(" ( ")
			.append(StringUtils.join(c.getCriterions(), " or "))
			.append(" ) ");
		
		c.setJunction(oldJ);
		c.setCriterions(criterions);
	}

	@Override
	protected void parseSingleValueFilterCriterion(CriteriaContext context) {
		QLCriteriaContext criteriaContext = (QLCriteriaContext) context;
		SingleValueFilterCriterion criterion = criteriaContext.getCurrent();
		List<CriterionParser<StringBuilder>> criterionParsers = criteriaContext.getCriterionParsers();
		String property = criterion.getProperty();;
		String alias = criteriaContext.getAlias();
		AfterWhere afterWhere = criteriaContext.getAfterWhere();
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

}
