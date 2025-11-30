package com.craftinginterpreters.lox

class Interpreter : Expr.Visitor<Object?>, Stmt.Visitor<Unit> {
  private var environment = Environment()

  fun interpret(statements: List<Stmt?>) {
    try {
      for (stmt in statements) {
        stmt?.let { execute(it) }
      }
    } catch (error: RuntimeError) {
      Lox.runtimeError(error)
    }
  }

  override fun visitBinaryExpr(expr: Expr.Binary): Object? {
    val left = evaluate(expr.left)
    val right = evaluate(expr.right)

    return when (val type = expr.operator.type) {
      TokenType.MINUS, TokenType.STAR ->
        (arithOps[type]?.let { func ->
          checkNumberOperands(expr.operator, left, right).let {(left, right) -> func(left, right) }
        }) as Object
      TokenType.SLASH ->
        checkNumberOperands(expr.operator, left, right).let {(left, right) -> division(left, right)}
            ?: throw RuntimeError(expr.operator, "Cannot divide by 0")
      TokenType.PLUS -> addition(left, right) ?:
        throw RuntimeError(expr.operator, "Addition operands must both be two doubles or two strings.")
      TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL ->
        (boolOps[type]?.let { func ->
          checkNumberOperands(expr.operator, left, right).let {(left, right) -> func(left, right) }
        }) as Object
      TokenType.EQUAL_EQUAL -> isEqual(left, right) as Object
      TokenType.BANG_EQUAL -> !isEqual(left, right) as Object
      else -> null
    }
  }

  override fun visitGroupingExpr(expr: Expr.Grouping): Object? {
    return evaluate(expr.expression)
  }

  override fun visitLiteralExpr(expr: Expr.Literal): Object? {
    return expr.value
  }

  override fun visitUnaryExpr(expr: Expr.Unary): Object? {
    val right = evaluate(expr.right)

    return when (expr.operator.type) {
      TokenType.MINUS -> -checkNumberOperand(expr.operator, right) as Object
      TokenType.BANG -> !isTruthy(right) as Object
      else -> null
    }
  }

  override fun visitTernaryExpr(expr: Expr.Ternary): Object? {
    val condition = evaluate(expr.condition)
    if (isTruthy(condition)) {
      return evaluate(expr.ifTrue)
    }
    return evaluate(expr.ifFalse)
  }

  override fun visitExpressionStmt(stmt: Stmt.Expression) {
    evaluate(stmt.expression)
    return
  }

  override fun visitPrintStmt(stmt: Stmt.Print) {
    val value = evaluate(stmt.expression)
    println(stringify(value))
  }

  override fun visitVariableExpr(expr: Expr.Variable): Object? {
    return environment[expr.name]
  }

  override fun visitVarStmt(stmt: Stmt.Var) {
    val value = stmt.initializer?.run { evaluate(this) }
    environment[stmt.name.lexeme] = value
  }

  override fun visitAssignExpr(expr: Expr.Assign): Object? {
    val value = evaluate(expr.value)
    environment.assign(expr.name, value)
    return value
  }

  override fun visitBlockStmt(stmt: Stmt.Block) {
    executeBlock(stmt.statements, Environment(this.environment))
  }

  private fun executeBlock(stmtList: List<Stmt?>, environment: Environment) {
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

  private fun evaluate(expr: Expr): Object? {
    return expr.accept(this)
  }

  private fun stringify(obj: Object?): String {
    return obj?.run {
      var text = obj.toString()
      if (obj is Double) {
        text = text.takeIf { it.endsWith(".0") }?.run { this.substring(0, this.length - 2) } ?: text
      }
      text
    } ?: "nil"
  }

  private fun isTruthy(obj: Object?): Boolean {
    if (obj == null) return false
    if (obj is Boolean) return obj as Boolean
    return true;
  }

  private fun addition(left: Object?, right: Object?): Object? {
    if (left is Double && right is Double) return ((left as Double) + (right as Double)) as Object
    if (left is String && right is String) return ((left as String) + (right as String)) as Object
    if (left is String) return (left + stringify(right)) as Object
    if (right is String) return (stringify(left) + right) as Object

    return null
  }

  private fun division(left: Double, right: Double): Object? {
    return right.takeIf { it != 0.0 }?.run { (left / this) as Object }
  }

  private fun isEqual(left: Object?, right: Object?): Boolean {
    if (left == null && right == null) return true
    if (left == null) return false
    return left == right
  }

  private fun checkNumberOperand(operator: Token, operand: Object?): Double {
    if (operand is Double) return operand as Double
    throw RuntimeError(operator, "Operand must be a number.");
  }

  private fun checkNumberOperands(operator: Token, left: Object?, right: Object?): Pair<Double, Double> {
    if (left is Double && right is Double) return Pair(left as Double, right as Double)
    throw RuntimeError(operator, "Operands must be numbers.");
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