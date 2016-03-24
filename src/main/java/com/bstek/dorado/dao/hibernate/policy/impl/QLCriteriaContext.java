package com.bstek.dorado.dao.hibernate.policy.impl;


import java.util.ArrayList;
import java.util.List;

import com.bstek.dorado.dao.AfterWhere;
import com.bstek.dorado.dao.hibernate.parser.CriterionParser;
import com.bstek.dorado.dao.hibernate.policy.CriteriaContext;
import com.bstek.dorado.data.provider.Criteria;
import com.bstek.dorado.data.provider.Junction;

/**
 *@author Kevin.yang
 *@since 2015年5月23日
 */
public class QLCriteriaContext implements CriteriaContext {
	private AfterWhere afterWhere;
	private Object current;
	private Junction junction;
	private Class<?> entityClass;
	private String alias;
	private Criteria criteria;
	private List<String> criterions;
	private List<String> orders;
	private List<CriterionParser<StringBuilder>> criterionParsers = new ArrayList<CriterionParser<StringBuilder>>();
	
	
	
	
	@Override
	public Criteria getCriteria() {
		return criteria;
	}
	
	public void setCriteria(Criteria criteria) {
		this.criteria = criteria;
	}


	@Override
	public <E> void setCurrent(E current) {
		this.current = current;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E getCurrent() {
		return (E) current;
	}
	
	public Junction getJunction() {
		return junction;
	}

	public void setJunction(Junction junction) {
		this.junction = junction;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public void setEntityClass(Class<?> entityClass) {
		this.entityClass = entityClass;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public AfterWhere getAfterWhere() {
		return afterWhere;
	}

	public void setAfterWhere(AfterWhere afterWhere) {
		this.afterWhere = afterWhere;
	}

	public List<String> getCriterions() {
		if(criterions == null) {
			criterions = new ArrayList<String>();
		}
		return criterions;
	}

	public void setCriterions(List<String> criterions) {
		this.criterions = criterions;
	}

	public List<String> getOrders() {
		if(orders == null) {
			orders = new ArrayList<String>();
		}
		return orders;
	}

	public void setOrders(List<String> orders) {
		this.orders = orders;
	}

	public List<CriterionParser<StringBuilder>> getCriterionParsers() {
		return criterionParsers;
	}

	public void setCriterionParsers(
			List<CriterionParser<StringBuilder>> criterionParsers) {
		this.criterionParsers = criterionParsers;
	}

	
	
	

}
