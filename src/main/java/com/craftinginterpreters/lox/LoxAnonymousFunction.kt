package com.craftinginterpreters.lox

class LoxAnonymousFunction(val expr: Expr.Lambda, val closure: Environment?) : LoxCallable {

  override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
    val environment = Environment(closure)
    for (idx in 0..< arity()) {
      environment.define(arguments[idx])
    }
    try {
      interpreter.executeBlock(expr.body, environment)
    } catch (ret: ControlFlowException.Return) {
      return ret.value
    }
    return null
  }

  override fun arity(): Int = expr.params.size

  override fun toString(): String = "<anonymous_fn @${expr.head.line}>"
}