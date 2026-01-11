package com.craftinginterpreters.lox

class LoxFunction(val declaration: Stmt.Function, val closure: Environment?, val isInitializer: Boolean) : LoxCallable {
  override fun call(interpreter: Interpreter, arguments: List<Any?>, ): Any? {
    val environment = Environment(closure)
    for (idx in 0 ..< arity()) {
      environment.define(arguments[idx])
    }

    try {
      interpreter.executeBlock(declaration.body, environment)
    } catch(ret: ControlFlowException.Return) {
      if (isInitializer) { return closure?.getAt(0, 0) }
      return ret.value
    }

    if (isInitializer) { return closure?.getAt(0, 0) }
    return null
  }

  fun bind(instance: LoxInstance): LoxFunction {
    val environment = Environment(closure)
    environment.define(instance)
    return LoxFunction(declaration, environment, isInitializer)
  }

  fun isGetter() = declaration.params == null

  override fun arity(): Int = declaration.params?.size ?: 0

  override fun toString(): String = "<fn ${declaration.name.lexeme}>"
}