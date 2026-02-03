package com.craftinginterpreters.lox

class LoxClass(
    val name: String, val superclass: LoxClass?, val methods: HashMap<String, LoxFunction>,
    staticMethods: HashMap<String, LoxFunction>?,
) : LoxCallable, LoxInstance(staticMethods?.let {LoxClass("$name metaclass", null, it, null)}) {

  fun findMethod(name: String, instance: LoxInstance): LoxFunction? {
    var klass = instance.klass
    var method: LoxFunction? = null
    var inner: LoxFunction? = null
    while (klass != null) {
      klass.methods[name]?.let {
        if (inner == null) {
          inner = method
        }
        method = it
      }
      klass = klass.superclass
    }
    if (method != null) {
      return method.bind(instance, inner)
    }
    return null
  }

  override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
    val instance = LoxInstance(this)
    val initializer = findMethod("init", instance)
    initializer?.call(interpreter, arguments)

    return instance
  }

  override fun arity(): Int {
    val initializer = findMethod("init", LoxInstance(this))
    return initializer?.arity() ?: 0
  }
}