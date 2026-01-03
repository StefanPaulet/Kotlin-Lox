package com.craftinginterpreters.lox

import java.util.Stack

class Resolver(val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
  private enum class FunctionType {
    NONE,
    FUNCTION,
    LAMBDA,
  }

  private val scopes = Stack<MutableMap<String, Boolean>>()
  private var currentFunction = FunctionType.NONE
  private var inLoop = false
  private val usages = HashSet<Token>()

  fun resolve(statements: List<Stmt?>) = statements.forEach { resolve(it) }

  override fun visitBlockStmt(stmt: Stmt.Block) = inScope { resolve(stmt.statements) }

  override fun visitVarStmt(stmt: Stmt.Var) {
    declare(stmt.name)
    stmt.initializer?.let { resolve(it) }
    define(stmt.name)
  }

  override fun visitVariableExpr(expr: Expr.Variable) {
    if (scopes.isNotEmpty() && scopes.peek()[expr.name.lexeme] == false) {
      Lox.error(expr.name, "Can't read local variable in its own initializer.")
    }

    resolveLocal(expr, expr.name)
    usages.removeIf { it.lexeme == expr.name.lexeme }
  }

  override fun visitAssignExpr(expr: Expr.Assign) {
    resolve(expr.value)
    resolveLocal(expr, expr.name)
  }

  override fun visitFunctionStmt(stmt: Stmt.Function) {
    declare(stmt.name)
    define(stmt.name)

    resolveFunction(stmt, FunctionType.FUNCTION)
  }

  private fun resolve(statement: Stmt?) {
    statement?.accept(this)
  }

  private fun resolve(expression: Expr) {
    expression.accept(this)
  }

  private fun resolveLocal(expr: Expr, name: Token) {
    for (idx in scopes.size - 1 downTo 0) {
      if (scopes[idx].containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size - 1 - idx)
      }
    }
  }

  private fun resolveFunction(func: Stmt.Function, type: FunctionType) {
    inFunction(type) {
      inScope {
        for (param in func.params) {
          declare(param)
          define(param)
        }
        resolve(func.body)
      }
    }
  }

  override fun visitLambdaExpr(expr: Expr.Lambda) {
    inFunction(FunctionType.LAMBDA) {
      inScope {
        for (param in expr.params) {
          declare(param)
          define(param)
        }
        resolve(expr.body)
      }
    }
  }

  override fun visitExpressionStmt(stmt: Stmt.Expression) = resolve(stmt.expression)
  override fun visitIfStmt(stmt: Stmt.If) {
    resolve(stmt.condition)
    resolve(stmt.thenBranch)
    stmt.elseBranch?.let { resolve(it) }
  }
  override fun visitPrintStmt(stmt: Stmt.Print) = resolve(stmt.expression)
  override fun visitReturnStmt(stmt: Stmt.Return) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Can't return from top-level code")
    }
    stmt.expression?.let { resolve(it) } ?: Unit
  }
  override fun visitWhileStmt(stmt: Stmt.While) {
    resolve(stmt.condition)
    inLoop { resolve(stmt.body) }
  }
  override fun visitBreakStmt(stmt: Stmt.Break) {
    if (!inLoop) { Lox.error(stmt.keyword, "Break statement may not appear outside of a loop.") }
  }

  override fun visitBinaryExpr(expr: Expr.Binary) {
    resolve(expr.left)
    resolve(expr.right)
  }
  override fun visitCallExpr(expr: Expr.Call) {
    resolve(expr.callee)
    expr.arguments.forEach { resolve(it) }
  }
  override fun visitGroupingExpr(expr: Expr.Grouping) = resolve(expr.expression)
  override fun visitLiteralExpr(expr: Expr.Literal) = Unit
  override fun visitLogicalExpr(expr: Expr.Logical) {
    resolve(expr.left)
    resolve(expr.right)
  }
  override fun visitUnaryExpr(expr: Expr.Unary) = resolve(expr.right)
  override fun visitTernaryExpr(expr: Expr.Ternary) {
    resolve(expr.condition)
    resolve(expr.ifTrue)
    resolve(expr.ifFalse)
  }

  private fun declare(name: Token) {
    if (scopes.isEmpty()) return

    val scope = scopes.peek()
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name, "A variable with this name already exists in this scope")
    }
    scope[name.lexeme] = false
    usages.add(name)
  }

  private fun define(name: Token) {
    if (scopes.isEmpty()) return
    scopes.peek()[name.lexeme] = true
  }

  private fun <R> inScope(callable: () -> R): R {
    beginScope()
    val retVal = callable()
    endScope()

    return retVal
  }

  private fun <R> inLoop(callable: () -> R): R {
    inLoop = true
    val retVal = callable()
    inLoop = false

    return retVal
  }

  private fun <R> inFunction(type: FunctionType, callable: () -> R): R {
    val enclosingFunction = currentFunction
    currentFunction = type
    val retVal = callable()
    currentFunction = enclosingFunction

    return retVal
  }

  private fun beginScope() {
    scopes.push(HashMap())
    usages.clear()
  }
  private fun endScope() {
    scopes.pop()
    usages.forEach {
      Lox.warning(it, "Unused local variable")
    }
  }
}