package com.bstek.dorado.dao.hibernate.hibernate3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;

import com.bstek.dorado.core.Configure;
import com.bstek.dorado.dao.Constants;
import com.bstek.dorado.dao.hibernate.MappingResourcesRegister;
import com.bstek.dorado.dao.hibernate.PackagesToScanRegister;


/**
 *@author Kevin.yang
 *@since 2015年5月16日
 */
public class EnhanceAnnotationSessionFactoryBean extends
		AnnotationSessionFactoryBean implements ApplicationContextAware, BeanNameAware {
	private List<String> mappingResources = new ArrayList<String>();
	private List<String> packagesToScan = new ArrayList<String>();
	private boolean asDefault;
	private String beanName;
	private ApplicationContext applicationContext;
	
	@Override
	public void setMappingResources(String[] mappingResources) {
		if (mappingResources != null) {
			for (String mappingResource : mappingResources) {
				this.mappingResources.add(mappingResource);
			}
		}
	}
	
	@Override
	public void setPackagesToScan(String[] packagesToScan) {
		if (packagesToScan != null) {
			for (String p : packagesToScan) {
				this.packagesToScan.add(p);
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Collection<MappingResourcesRegister> allMappingResources = 
				applicationContext.getBeansOfType(MappingResourcesRegister.class).values();
		for (MappingResourcesRegister mappingResources : allMappingResources) {
			String sessionFactoryName = mappingResources.getSessionFactoryName();
			if (support(sessionFactoryName)) {
				this.mappingResources.addAll(mappingResources.getMappingResources());
			}
		}
		
		if (mappingResources.size()>0) {
			super.setMappingResources(mappingResources.toArray(new String[mappingResources.size()]));
		}
		
		Collection<PackagesToScanRegister> packagesToScan = 
				applicationContext.getBeansOfType(PackagesToScanRegister.class).values();
		for (PackagesToScanRegister p : packagesToScan) {
			String sessionFactoryName = p.getSessionFactoryName();
			if (support(sessionFactoryName)) {
				this.packagesToScan.addAll(p.getPackagesToScan());
			}
		}
		if (this.packagesToScan.size()>0) {
			super.setPackagesToScan(this.packagesToScan.toArray(new String[this.packagesToScan.size()]));
		}
		super.afterPropertiesSet();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
		
	}

	public void setAsDefault(boolean asDefault) {
		this.asDefault = asDefault;
	}
	
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
		String defautSessionFactoryName = Configure.getString(Constants.DEFAULT_SESSION_FACTORY_PROP);
		if (StringUtils.isBlank(defautSessionFactoryName) || beanName.equals(defautSessionFactoryName)) {
			this.asDefault = true;
		}
	}
	
	private boolean support(String sessionFactoryName) {
		if (beanName.equals(sessionFactoryName) || StringUtils.isBlank(sessionFactoryName) && asDefault) {
			return true;
		}
		return false;
	}
	
}
