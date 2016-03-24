package com.bstek.dorado.dao.hibernate;

import java.util.List;

/**
 *@author Kevin.yang
 *@since 2015年5月16日
 */
public class PackagesToScanRegister {
	private List<String> packagesToScan;
	private String sessionFactoryName;
	
	public List<String> getPackagesToScan() {
		return packagesToScan;
	}
	public void setPackagesToScan(List<String> packagesToScan) {
		this.packagesToScan = packagesToScan;
	}
	public String getSessionFactoryName() {
		return sessionFactoryName;
	}
	public void setSessionFactoryName(String sessionFactoryName) {
		this.sessionFactoryName = sessionFactoryName;
	}
	
	
}
