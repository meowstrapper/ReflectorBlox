package com.drake.reflectorblox.dexactivity

import java.lang.reflect.Field
import java.lang.reflect.Method

fun getField(clazz: Class<*>, name: String): Field {
    clazz.getField(name).let {
        if (!it.isAccessible) {
            it.isAccessible = true
        }
        return it
    }
}

fun getMethod(clazz: Class<*>, method: String, vararg parameterTypes: Class<*>): Method {
    clazz.getMethod(method, *parameterTypes).let {
        if (!it.isAccessible) {
            it.isAccessible = true
        }
        return it
    }
}