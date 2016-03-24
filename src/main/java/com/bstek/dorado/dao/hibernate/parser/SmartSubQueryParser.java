package com.bstek.dorado.dao.hibernate.parser;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Criterion;

import com.bstek.dorado.dao.hibernate.HibernateUtils;
import com.bstek.dorado.dao.hibernate.query.CollectInfo;
import com.bstek.dorado.data.provider.filter.SingleValueFilterCriterion;

/**
 *@author Kevin.yang
 *@since 2015年8月16日
 */
public class SmartSubQueryParser implements CriterionParser<org.hibernate.criterion.Criterion> {

	private List<CriterionParser<Criterion>> parsers = new ArrayList<CriterionParser<Criterion>>(3);
	
	public SmartSubQueryParser(Class<?> entityClass, List<CollectInfo> collectInfos, String alias) {
		if (StringUtils.isEmpty(alias)) {
			alias = "__alias__";
		}
		PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(entityClass);
		for (PropertyDescriptor pd : pds) {
			if (HibernateUtils.isEntityClass(pd.getPropertyType())) {
				boolean found = false;
				for (CollectInfo collectInfo : collectInfos) {
					Class<?> cls = collectInfo.getEntityClass();
					if (cls != null && pd.getPropertyType().isAssignableFrom(cls)) {
						parsers.add(new SubQueryParser(cls, alias, collectInfo.getProperties()));
						found = true;
						break;
					}
				}
				if (!found) {
					parsers.add(new SubQueryParser(pd.getPropertyType(), alias));
				}
			}
		}
	}
	
	@Override
	public org.hibernate.criterion.Criterion parse(SingleValueFilterCriterion criterion) {
		org.hibernate.criterion.Criterion c = null;
		for (CriterionParser<Criterion> parser : parsers) {
			c = parser.parse(criterion);
			if (c != null) {
				return c;
			}
		}
				
		return null;
	}

}
