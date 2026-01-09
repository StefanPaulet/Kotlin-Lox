package com.craftinginterpreters.lox

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
  private class VarBinding(val depth: Int, var slot: Int)

  val globals = GlobalEnvironment()
  private var environment: Environment? = null
  private val locals = HashMap<Expr, VarBinding>()

  constructor() {
    globals["clock"] = object : LoxCallable {
      override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        return System.currentTimeMillis() / 1000.0
      }

      override fun arity(): Int = 0
    }
  }

  fun interpret(statements: List<Stmt?>) {
    try {
      for (stmt in statements) {
        stmt?.let { execute(it) }
      }
    } catch (error: RuntimeError) {
      Lox.runtimeError(error)
    }
  }

  fun interpret(expr: Expr): String? {
    try {
      return stringify(evaluate(expr))
    } catch (error: RuntimeError) {
      Lox.runtimeError(error)
    }
    return null
  }

  override fun visitBinaryExpr(expr: Expr.Binary): Any? {
    val left = evaluate(expr.left)
    val right = evaluate(expr.right)

    return when (val type = expr.operator.type) {
      TokenType.MINUS, TokenType.STAR ->
        (arithOps[type]?.let { func ->
          checkNumberOperands(expr.operator, left, right).let {(left, right) -> func(left, right) }
        })
      TokenType.SLASH ->
        checkNumberOperands(expr.operator, left, right).let {(left, right) -> division(left, right)}
            ?: throw RuntimeError(expr.operator, "Cannot divide by 0")
      TokenType.PLUS -> addition(left, right) ?:
        throw RuntimeError(expr.operator, "Addition operands must both be two doubles or two strings.")
      TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL ->
        (boolOps[type]?.let { func ->
          checkNumberOperands(expr.operator, left, right).let {(left, right) -> func(left, right) }
        })
      TokenType.EQUAL_EQUAL -> isEqual(left, right)
      TokenType.BANG_EQUAL -> !isEqual(left, right)
      TokenType.COMMA -> right
      else -> null
    }
  }

  override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
    return evaluate(expr.expression)
  }

  override fun visitLiteralExpr(expr: Expr.Literal): Any? {
    return expr.value
  }

  override fun visitUnaryExpr(expr: Expr.Unary): Any? {
    val right = evaluate(expr.right)

    return when (expr.operator.type) {
      TokenType.MINUS -> -checkNumberOperand(expr.operator, right) as Any
      TokenType.BANG -> !isTruthy(right) as Any
      else -> null
    }
  }

  override fun visitLambdaExpr(expr: Expr.Lambda): Any {
    return LoxAnonymousFunction(expr, environment)
  }

  override fun visitCallExpr(expr: Expr.Call): Any? {
    val callee = evaluate(expr.callee)
    val arguments = mutableListOf<Any?>()
    for (arg in expr.arguments) {
      arguments.add(evaluate(arg))
    }

    if (callee !is LoxCallable) {
      throw RuntimeError(expr.paren, "Can only call functions and classes.")
    }

    if (arguments.size != callee.arity()) {
      throw RuntimeError(expr.paren, "Expected ${callee.arity()} arguments but got ${arguments.size}.")
    }

    return callee.call(this, arguments)
  }

  override fun visitGetExpr(expr: Expr.Get): Any? {
    val instance = evaluate(expr.instance)
    if (instance !is LoxInstance) {
      throw RuntimeError(expr.name, "Only instances have properties")
    }

    return instance.get(expr.name)
  }

  override fun visitTernaryExpr(expr: Expr.Ternary): Any? {
    val condition = evaluate(expr.condition)
    if (isTruthy(condition)) {
      return evaluate(expr.ifTrue)
    }
    return evaluate(expr.ifFalse)
  }

  override fun visitClassStmt(stmt: Stmt.Class) {
    val methods = HashMap<String, LoxFunction>()
    for (method in stmt.methods) {
      val function = LoxFunction(method, environment, method.name.lexeme == "init")
      methods[method.name.lexeme] = function
    }

    val loxClass = LoxClass(stmt.name.lexeme, methods)
    define(stmt.name, loxClass)
  }

  override fun visitFunctionStmt(stmt: Stmt.Function) {
    val function = LoxFunction(stmt, environment, false)
    define(stmt.name, function)
  }

  override fun visitVarStmt(stmt: Stmt.Var) {
    stmt.initializer?.run {
      define(stmt.name, evaluate(this))
    } ?:run {
      declare(stmt.name)
    }
  }

  override fun visitExpressionStmt(stmt: Stmt.Expression) {
    evaluate(stmt.expression)
    return
  }

  override fun visitIfStmt(stmt: Stmt.If) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch)
    } else if (stmt.elseBranch != null) {
      execute(stmt.thenBranch)
    }
    return
  }

  override fun visitPrintStmt(stmt: Stmt.Print) {
    val value = evaluate(stmt.expression)
    println(stringify(value))
  }

  override fun visitReturnStmt(stmt: Stmt.Return) {
    val value = stmt.expression?.let { evaluate(it) }
    throw ControlFlowException.Return(value)
  }

  override fun visitWhileStmt(stmt: Stmt.While) {
    while(isTruthy(evaluate(stmt.condition))) {
      try {
        execute(stmt.body)
      } catch (_: ControlFlowException.Break) {
        break
      }
    }
  }

  override fun visitBreakStmt(stmt: Stmt.Break) {
    throw ControlFlowException.Break()
  }

  override fun visitBlockStmt(stmt: Stmt.Block) {
    executeBlock(stmt.statements, Environment(this.environment))
  }

  override fun visitAssignExpr(expr: Expr.Assign): Any? {
    val value = evaluate(expr.value)
    val distance = locals[expr]
    distance?.let {
      if (environment == null) { throw InternalCompilerError("static name resolution failed") }
      environment!!.assignAt(distance.depth, distance.slot, value)
    } ?:let { globals.assign(expr.name, value) }
    return value
  }

  override fun visitSetExpr(expr: Expr.Set): Any? {
    val instance = evaluate(expr.instance)
    if (instance !is LoxInstance) {
      throw RuntimeError(expr.name, "Only instances have fields.");
    }

    val value = evaluate(expr.value)
    instance.set(expr.name, value)
    return value
  }

  override fun visitThisExpr(expr: Expr.This): Any? {
    return lookUpVariable(expr.keyword, expr)
  }

  override fun visitLogicalExpr(expr: Expr.Logical): Any? {
    val left = evaluate(expr.left)

    when (expr.operator.type) {
      TokenType.OR -> if (isTruthy(left)) return left
      TokenType.AND -> if (!isTruthy(left)) return left
      else -> Unit
    }

    return evaluate(expr.right)
  }

  override fun visitVariableExpr(expr: Expr.Variable): Any? {
    return lookUpVariable(expr.name, expr)
  }

  fun executeBlock(stmtList: List<Stmt?>, environment: Environment) {
    val previous = this.environment
    try {
      this.environment = environment
      for (stmt in stmtList) {
        stmt?.let { execute(it) }
      }
    } finally {
      this.environment = previous
    }
  }

  private fun execute(stmt: Stmt) {
    stmt.accept(this)
  }

  private fun evaluate(expr: Expr): Any? {
    return expr.accept(this)
  }

  private fun stringify(obj: Any?): String {
    return obj?.run {
      var text = obj.toString()
      if (obj is Double) {
        text = text.takeIf { it.endsWith(".0") }?.run { this.substring(0, this.length - 2) } ?: text
      }
      text
    } ?: "nil"
  }

  private fun isTruthy(obj: Any?): Boolean {
    if (obj == null) return false
    if (obj is Boolean) return obj
    return true
  }

  private fun addition(left: Any?, right: Any?): Any? {
    if (left is Double && right is Double) return (left + right) as Any
    if (left is String && right is String) return (left + right) as Any
    if (left is String) return (left + stringify(right)) as Any
    if (right is String) return (stringify(left) + right) as Any

    return null
  }

  private fun division(left: Double, right: Double): Any? {
    return right.takeIf { it != 0.0 }?.run { left / this }
  }

  private fun isEqual(left: Any?, right: Any?): Boolean {
    if (left == null && right == null) return true
    if (left == null) return false
    return left == right
  }

  private fun checkNumberOperand(operator: Token, operand: Any?): Double {
    if (operand is Double) return operand
    throw RuntimeError(operator, "Operand must be a number.")
  }

  private fun checkNumberOperands(operator: Token, left: Any?, right: Any?): Pair<Double, Double> {
    if (left is Double && right is Double) return Pair(left, right)
    throw RuntimeError(operator, "Operands must be numbers.")
  }

  fun resolve(expr: Expr, envOffset: Int, scopeOffset: Int) {
    locals[expr] = VarBinding(envOffset, scopeOffset)
  }

  private fun lookUpVariable(name: Token, expr: Expr): Any? {
    val distance = locals[expr]
    return if (distance != null) run {
      if (environment == null) { throw InternalCompilerError("static name resolution failed") }
      environment!!.getAt(distance.depth, distance.slot)
    } else globals[name]
  }

  private fun declare(name: Token) {
    environment?.declare()
        ?: run { globals[name.lexeme] = null }
  }

  private fun define(name: Token, value: Any?) {
    environment?.apply { this.define(value) }
        ?: run { globals[name.lexeme] = value }
  }

  companion object {
    val arithOps = mapOf<TokenType, (Double, Double) -> Double>(
      TokenType.MINUS to { lhs, rhs -> lhs - rhs },
      TokenType.STAR to { lhs, rhs -> lhs * rhs },
    )

    val boolOps = mapOf<TokenType, (Double, Double) -> Boolean>(
      TokenType.GREATER to { lhs, rhs -> lhs > rhs },
      TokenType.GREATER_EQUAL to { lhs, rhs -> lhs >= rhs },
      TokenType.LESS to { lhs, rhs -> lhs < rhs },
      TokenType.LESS_EQUAL to { lhs, rhs -> lhs <= rhs },
    )
  }
}