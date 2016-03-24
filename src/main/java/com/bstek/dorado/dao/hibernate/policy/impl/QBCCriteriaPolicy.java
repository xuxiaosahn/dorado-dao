package com.bstek.dorado.dao.hibernate.policy.impl;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Restrictions;

import com.bstek.dorado.dao.CriteriaUtils;
import com.bstek.dorado.dao.FieldUtils;
import com.bstek.dorado.dao.hibernate.parser.CriterionParser;
import com.bstek.dorado.dao.hibernate.policy.CriteriaContext;
import com.bstek.dorado.data.provider.And;
import com.bstek.dorado.data.provider.Or;
import com.bstek.dorado.data.provider.Order;
import com.bstek.dorado.data.provider.filter.SingleValueFilterCriterion;

/**
 *@author Kevin.yang
 *@since 2015年5月23日
 */
public class QBCCriteriaPolicy extends AbstractCriteriaPolicy<DetachedCriteria> {

	@Override
	public DetachedCriteria apply(CriteriaContext context) {
		QBCCriteriaContext c = (QBCCriteriaContext) context;
		DetachedCriteria dc = c.getDetachedCriteria();
		if (dc == null) {
			if (StringUtils.isEmpty(c.getAlias())) {
				dc = DetachedCriteria.forClass(c.getEntityClass());
			} else {
				dc = DetachedCriteria.forClass(c.getEntityClass(), c.getAlias());
			}
		}
		if(c.getCriteria() != null) {
			Junction junction = Restrictions.conjunction();
			c.setCriteria(c.getCriteria());
			c.setDetachedCriteria(dc);
			c.setJunction(junction);
			c.setCurrent(c.getCriteria().getCriterions());
			parseCriterions(c);
			parseOrders(c);
			dc.add(junction);
		}
		return dc;		
	}

	@Override
	protected void parseOrder(CriteriaContext context) {
		QBCCriteriaContext c = (QBCCriteriaContext) context;
		Order order = c.getCurrent();
		DetachedCriteria dc = c.getDetachedCriteria();
		if (order.isDesc()) {
			dc.addOrder(org.hibernate.criterion.Order.desc(order.getProperty()));
		} else {
			dc.addOrder(org.hibernate.criterion.Order.asc(order.getProperty()));
		}
		
	}

	@Override
	protected void parseAndCriterion(CriteriaContext context) {
		QBCCriteriaContext c = (QBCCriteriaContext) context;
		Junction oldJ = c.getJunction();
		And and = c.getCurrent();

		Junction j = Restrictions.conjunction();
		oldJ.add(j);
		c.setJunction(j);
		c.setCurrent(and.getCriterions());
		parseCriterions(c);
		c.setJunction(oldJ);
	}

	@Override
	protected void parseOrCriterion(CriteriaContext context) {
		QBCCriteriaContext c = (QBCCriteriaContext) context;
		Junction oldJ = c.getJunction();
		Or or = c.getCurrent();
		
		Junction j = Restrictions.disjunction();
		oldJ.add(j);
		c.setJunction(j);
		c.setCurrent(or.getCriterions());
		parseCriterions(c);
		c.setJunction(oldJ);
		
	}

	@Override
	protected void parseSingleValueFilterCriterion(CriteriaContext context) {
		QBCCriteriaContext criteriaContext = (QBCCriteriaContext) context;
		List<CriterionParser<Criterion>> criterionParsers = criteriaContext.getCriterionParsers();
		SingleValueFilterCriterion criterion = criteriaContext.getCurrent();
		org.hibernate.criterion.Criterion c =null;
		String alias = criteriaContext.getAlias();
		String property = criterion.getProperty();
		Object value = criterion.getValue();
		Class<?> cls = criteriaContext.getEntityClass();
		
		for (CriterionParser<Criterion> criterionParser : criterionParsers) {
			c = criterionParser.parse(criterion);
			if (c != null) {
				break;
			}
		}
		
		
		if (c == null) {
			if (cls != null) {
				Field field = FieldUtils.getField(cls, property);
				if (Enum.class.isAssignableFrom(field.getType()) 
						&& value instanceof String) {
					Class<?> type = field.getType();
					Enum<?>[] items = (Enum<?>[]) type.getEnumConstants();
					if(items!=null){
						for(Enum<?> item:items){
							if (item.name().equals(value)) {
								criterion.setValue(item);
								break;
							}
						}
					}
				}

			}
			if (StringUtils.isNotEmpty(alias)) {
				property = alias + "." + property;
			}
			int index = property.lastIndexOf('.');
			if (index > 0) {
				String associationPath = property.substring(0, index);
				alias = associationPath.replaceAll("\\.", StringUtils.EMPTY);
				property = alias + "." + property.substring(index + 1);
				criteriaContext.getDetachedCriteria().createAlias(associationPath, alias);
				criterion.setProperty(property);
			}
			c = CriteriaUtils.parse(criterion);
		}

		criteriaContext.getJunction().add(c);
		
	}

}
