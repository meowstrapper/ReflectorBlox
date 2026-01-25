package com.drake.reflectorblox.dexactivity

import java.lang.reflect.Field
import java.lang.reflect.Method

fun getField(clazz: Class<*>, name: String): Field? {
    clazz.declaredFields.forEach {
        if (it.name.equals(name)) {
            if (!it.isAccessible) {
                it.isAccessible = true
            }
            return it
        }
    }
    return null
}

fun getMethod(clazz: Class<*>, method: String, vararg parameterTypes: Class<*>): Method {
    val classMethod = clazz.getDeclaredMethod(method, *parameterTypes)
    if (!classMethod.isAccessible) {
        classMethod.isAccessible = true
    }
    return classMethod
}