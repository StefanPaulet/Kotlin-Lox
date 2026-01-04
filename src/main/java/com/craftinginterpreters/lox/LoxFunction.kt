package com.craftinginterpreters.lox

class LoxFunction(val declaration: Stmt.Function, val closure: Environment?) : LoxCallable {
  override fun call(interpreter: Interpreter, arguments: List<Any?>, ): Any? {
    val environment = Environment(closure)
    for (idx in 0 ..< arity()) {
      environment.define(arguments[idx])
    }

    try {
      interpreter.executeBlock(declaration.body, environment)
    } catch(ret: ControlFlowException.Return) {
      return ret.value
    }

    return null
  }

  override fun arity(): Int = declaration.params.size

  override fun toString(): String = "<fn ${declaration.name.lexeme}>"
}