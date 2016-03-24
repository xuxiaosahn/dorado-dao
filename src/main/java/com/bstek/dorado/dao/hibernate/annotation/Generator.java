package com.bstek.dorado.dao.hibernate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bstek.dorado.dao.hibernate.policy.impl.GeneratorPolicy;
import com.bstek.dorado.dao.hibernate.policy.impl.UUIDPolicy;

/**
 *@author Kevin.yang
 *@since 2015年5月17日
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Generator {
	Class<? extends GeneratorPolicy> policy() default UUIDPolicy.class;
}
