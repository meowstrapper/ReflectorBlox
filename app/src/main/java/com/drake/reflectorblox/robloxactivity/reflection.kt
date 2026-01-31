package com.drake.reflectorblox.robloxactivity

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

fun getFields(
    clazz: Class<*>,
    isDeclared: Boolean = true
): List<Field> {
    return (if (isDeclared) {
        clazz.declaredFields
    } else {
        clazz.fields
    }).toList().apply {
        this.forEach { field ->
            field.isAccessible = true
        }
    }
}


fun getField(
    clazz: Class<*>,
    field: String,
    isDeclared: Boolean = true
): Field? {
    return (if (isDeclared) {
        clazz.getDeclaredField(field)
    } else {
        clazz.getField(field)
    }).apply {
        this.isAccessible = true
    }
}

fun getMethod(
    clazz: Class<*>,
    method: String,
    vararg parameterTypes: Class<*>,
    isDeclared: Boolean = true
): Method {
    return (if (isDeclared) {
        clazz.getDeclaredMethod(method, *parameterTypes)
    } else {
        clazz.getMethod(method, *parameterTypes)
    }).apply {
        this.isAccessible = true
    }
}

fun getConstructor(
    clazz: Class<*>,
    vararg parameterTypes: Class<*>
): Constructor<*> {
    return clazz.getDeclaredConstructor(*parameterTypes).apply {
        this.isAccessible = true
    }
}