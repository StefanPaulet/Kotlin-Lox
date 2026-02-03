package com.craftinginterpreters.lox

open class LoxInstance(val klass: LoxClass?) {
  private val fields = HashMap<String, Any?>()

  override fun toString(): String = "${klass?.name} instance"

  fun get(name: Token): Any? {
    if (fields.containsKey(name.lexeme)) {
      return fields[name.lexeme]
    }

    val method = klass?.findMethod(name.lexeme, this)
    if (method != null) return method

    throw RuntimeError(name,"Undefined property '${name.lexeme}'.")
  }

  fun set(name: Token, value: Any?) {
    fields[name.lexeme] = value
  }
}