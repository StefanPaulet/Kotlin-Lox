package com.craftinginterpreters.lox

class Environment(val enclosing: Environment? = null) {
  operator fun set(name: String, value: Any?) = values.put(name, value)

  operator fun get(name: Token): Any? = if (values.containsKey(name.lexeme)) values[name.lexeme] else
    enclosing?.get(name) ?:
    throw RuntimeError(name, "Undefined variable '${name.lexeme}'")

  fun getAt(distance: Int, name: Token): Any =
      ancestor(distance)[name] ?:
      throw InternalCompilerError("static name resolution failed for ${name.lexeme} on ${name.line}")

  fun declare(name: Token) = uninitialized.add(name.lexeme)

  fun assign(name: Token, value: Any?) {
    if (values.containsKey(name.lexeme)) {
      values[name.lexeme] = value
      return
    }
    if (uninitialized.contains(name.lexeme)) {
      values[name.lexeme] = value
      uninitialized.remove(name.lexeme)
      return
    }
    enclosing?.assign(name, value) ?: throw RuntimeError(name,"Undefined variable '${name.lexeme}'.")
  }

  fun assignAt(distance: Int, name: Token, value: Any?) {
    ancestor(distance).values[name.lexeme] = value
  }

  private fun ancestor(distance: Int): Environment {
    var env = this
    (0 ..< distance).forEach { _ ->
      env = env.enclosing ?: throw InternalCompilerError("environment depth resolution failed")
    }
    return env
  }

  private val values = HashMap<String, Any?>()
  private val uninitialized = HashSet<String>()
}