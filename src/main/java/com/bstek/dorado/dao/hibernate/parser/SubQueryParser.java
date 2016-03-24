package com.bstek.dorado.dao.hibernate.parser;

import java.beans.Introspector;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import com.bstek.dorado.dao.CriteriaUtils;
import com.bstek.dorado.dao.hibernate.HibernateUtils;
import com.bstek.dorado.data.provider.filter.SingleValueFilterCriterion;

/**
 *@author Kevin.yang
 *@since 2015年8月16日
 */
public class SubQueryParser implements CriterionParser<org.hibernate.criterion.Criterion> {

	private Class<?> entityClass;
	private String[] foreignKeys;
	private String alias;
	
	public SubQueryParser(Class<?> entityClass, String alias) {
		this.entityClass = entityClass;
		this.alias = alias;
		if (StringUtils.isEmpty(alias)) {
			this.alias = "__alias__";
		}
		
		this.foreignKeys = new String[]{ Introspector.decapitalize(entityClass.getSimpleName()) 
				+ StringUtils.capitalize(HibernateUtils.getIdPropertyName(entityClass)) };
	}
	
	public SubQueryParser(Class<?> entityClass, String alias, String... foreignKeys) {
		this(entityClass, alias);
		this.foreignKeys = foreignKeys;
		
	}
	
	@Override
	public org.hibernate.criterion.Criterion parse(SingleValueFilterCriterion criterion) {
		String property = criterion.getProperty();
		if (StringUtils.contains(property, '.')) {
			String alias = StringUtils.substringBefore(property, ".");
			
			for (String foreignKey : foreignKeys) {
				if (StringUtils.startsWith(alias, foreignKey)
						|| StringUtils.startsWith(foreignKey, alias)) {
					return Subqueries.exists(DetachedCriteria.forClass(entityClass, alias)
								.setProjection(Projections.id())
								.add(Restrictions.eqProperty(this.alias + "." + foreignKey, alias + "." + HibernateUtils.getIdPropertyName(entityClass)))
								.add(CriteriaUtils.parse(criterion)));
				}
			}
			
		}
				
		return null;
	}

}
