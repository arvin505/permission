package com.wangjing.permission.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by xiaoyi on 2017-8-31.
 */

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface PermissionsRationale {
    int[] value();
}
