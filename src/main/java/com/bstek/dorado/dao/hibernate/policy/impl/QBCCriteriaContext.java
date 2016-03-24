package com.bstek.dorado.dao.hibernate.policy.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Junction;

import com.bstek.dorado.dao.hibernate.parser.CriterionParser;
import com.bstek.dorado.dao.hibernate.policy.CriteriaContext;
import com.bstek.dorado.data.provider.Criteria;

/**
 *@author Kevin.yang
 *@since 2015年5月23日
 */
public class QBCCriteriaContext implements CriteriaContext {
	private DetachedCriteria detachedCriteria;
	private Object current;
	private Junction junction;
	private Class<?> entityClass;
	private String alias;
	private Criteria criteria;
	private List<CriterionParser<Criterion>> criterionParsers = new ArrayList<CriterionParser<Criterion>>();
	
	
	
	
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

	public DetachedCriteria getDetachedCriteria() {
		return detachedCriteria;
	}

	public void setDetachedCriteria(DetachedCriteria detachedCriteria) {
		this.detachedCriteria = detachedCriteria;
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

	public List<CriterionParser<Criterion>> getCriterionParsers() {
		return criterionParsers;
	}

	public void setCriterionParsers(
			List<CriterionParser<Criterion>> criterionParsers) {
		this.criterionParsers = criterionParsers;
	}

	
	
	

}
