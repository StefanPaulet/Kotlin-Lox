package com.craftinginterpreters.lox

class LoxClass(
    val name: String, val methods: HashMap<String, LoxFunction>,
    staticMethods: HashMap<String, LoxFunction>?,
) : LoxCallable, LoxInstance(staticMethods?.let {LoxClass("$name metaclass", it, null)}) {

  fun findMethod(name: String): LoxFunction? = methods[name]

  override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
    val instance = LoxInstance(this)
    val initializer = findMethod("init")
    initializer?.bind(instance)?.call(interpreter, arguments)

    return instance
  }

  override fun arity(): Int {
    val initializer = findMethod("init")
    return initializer?.arity() ?: 0
  }
}