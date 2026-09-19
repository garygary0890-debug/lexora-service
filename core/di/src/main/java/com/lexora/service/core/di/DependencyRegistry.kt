package com.lexora.service.core.di

import kotlin.reflect.KClass

class DependencyRegistry {
    private val singletonFactories = mutableMapOf<KClass<*>, () -> Any>()
    private val singletons = mutableMapOf<KClass<*>, Any>()

    fun <T : Any> registerSingleton(type: KClass<T>, provider: () -> T) {
        check(type !in singletonFactories && type !in singletons) { "Dependency already registered: ${type.qualifiedName}" }
        singletonFactories[type] = provider
    }

    fun <T : Any> registerInstance(type: KClass<T>, value: T) {
        check(type !in singletonFactories && type !in singletons) { "Dependency already registered: ${type.qualifiedName}" }
        singletons[type] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: KClass<T>): T {
        singletons[type]?.let { return it as T }
        val provider = singletonFactories[type] ?: error("Dependency is not registered: ${type.qualifiedName}")
        return provider().also { singletons[type] = it } as T
    }

    inline fun <reified T : Any> get(): T = get(T::class)
}
