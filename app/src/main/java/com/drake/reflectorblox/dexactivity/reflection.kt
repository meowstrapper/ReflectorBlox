package com.drake.reflectorblox.dexactivity

import java.lang.reflect.Field
import java.lang.reflect.Method

fun getField(clazz: Class<*>, name: String): Field? {
    try {
        clazz.getField(name).let {
            if (!it.isAccessible) {
                it.isAccessible = true
            }
            return it
        }
    } catch (e: NoSuchFieldException) {
        return null
    }
}

fun getMethod(clazz: Class<*>, method: String, vararg parameterTypes: Class<*>): Method? {
    try {
        clazz.getMethod(method, *parameterTypes).let {
            if (!it.isAccessible) {
                it.isAccessible = true
            }
            return it
        }
    } catch (e: NoSuchMethodException) {
        return null
    }
}