package org.deegree.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.deegree.spring.NullResourceFilter;
import org.deegree.spring.ResourceFilter;
import org.springframework.beans.factory.annotation.Autowired;

@Autowired
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectResource {
    String[] value() default {};
    Class<? extends ResourceFilter> filter() default NullResourceFilter.class;
}
