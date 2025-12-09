package com.craftinginterpreters.lox

class Environment(val enclosing: Environment? = null) {
  operator fun set(name: String, value: Any?) = values.put(name, value)

  operator fun get(name: Token): Any? = if (values.containsKey(name.lexeme)) values[name.lexeme] else
    enclosing?.get(name) ?:
    throw RuntimeError(name, "Undefined variable '${name.lexeme}'")

  fun assign(name: Token, value: Any?) {
    if (values.containsKey(name.lexeme)) {
      values[name.lexeme] = value
      return
    }
    enclosing?.assign(name, value) ?: throw RuntimeError(name,"Undefined variable '${name.lexeme}'.")
  }

  private val values = HashMap<String, Any?>()
}