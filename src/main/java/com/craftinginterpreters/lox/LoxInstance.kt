package com.craftinginterpreters.lox

open class LoxInstance(val klass: LoxClass?) {
  private val fields = HashMap<String, Any?>()

  override fun toString(): String = "${klass?.name} instance"

  open fun get(name: Token): Any? {
    if (fields.containsKey(name.lexeme)) {
      return fields[name.lexeme]
    }

    val method = klass?.findMethod(name.lexeme)
    if (method != null) return method.bind(this)

    throw RuntimeError(name,"Undefined property '${name.lexeme}'.")
  }

  open fun set(name: Token, value: Any?) {
    fields[name.lexeme] = value
  }
}