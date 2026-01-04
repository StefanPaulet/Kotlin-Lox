package com.craftinginterpreters.lox

class Environment(val enclosing: Environment? = null) {

  fun define(value: Any?) {
    values.add(value)
  }

  fun declare() {
    uninitialized.add(values.size)
    values.add(null)
  }

  fun getAt(distance: Int, position: Int): Any? {
    try {
      return ancestor(distance).values[position]
    } catch (_ : IndexOutOfBoundsException) {
      throw InternalCompilerError("static name resolution failed")
    }
  }

  fun assignAt(distance: Int, position: Int, value: Any?) {
    ancestor(distance).values[position] = value
  }

  private fun ancestor(distance: Int): Environment {
    var env = this
    (0 ..< distance).forEach { _ ->
      env = env.enclosing ?: throw InternalCompilerError("environment depth resolution failed")
    }
    return env
  }

  private val values = ArrayList<Any?>()
  private val uninitialized = HashSet<Int>()
}

class GlobalEnvironment {
  operator fun set(name: String, value: Any?) = values.put(name, value)

  operator fun get(name: Token): Any? = if (values.containsKey(name.lexeme)) values[name.lexeme] else
    throw RuntimeError(name, "Undefined variable '${name.lexeme}'")

  fun assign(name: Token, value: Any?) {
    if (values.containsKey(name.lexeme)) {
      values[name.lexeme] = value
      return
    }
    throw RuntimeError(name,"Undefined variable '${name.lexeme}'.")
  }

  private val values = HashMap<String, Any?>()
}