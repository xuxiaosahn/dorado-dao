package com.bstek.dorado.dao;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Where和Order部分结构化类。
 * 
 *@author Kevin.yang
 *@since 2015年5月20日
 */
public class AfterWhere {
	public final static String START_WHERE = " where 1=1 ";
	private Map<String,Object> paramMap = new LinkedHashMap<String,Object>();
	private StringBuilder wherePart = new StringBuilder();
	private StringBuilder orderPart = new StringBuilder();
	
	/**
	 * 返回命名参数Map对象。
	 */
	public Map<String, Object> getParamMap() {
		return paramMap;
	}
	
	/**
	 * 设置命名参数Map对象。
	 */
	public void setParamMap(Map<String, Object> paramMap) {
		this.paramMap = paramMap;
	}
	
	/**
	 * 返回查询语句Where部分。
	 */
	public StringBuilder getWherePart() {
		return wherePart;
	}
	
	/**
	 * 设置查询语句Where部分。
	 */
	public void setWherePart(StringBuilder wherePart) {
		this.wherePart = wherePart;
	}
	
	/**
	 * 返回查询语句Order部分。
	 */
	public StringBuilder getOrderPart() {
		return orderPart;
	}
	
	/**
	 * 设置查询语句Order部分。
	 */
	public void setOrderPart(StringBuilder orderPart) {
		this.orderPart = orderPart;
	}

}
