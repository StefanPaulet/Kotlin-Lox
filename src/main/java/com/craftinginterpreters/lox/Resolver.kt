package com.craftinginterpreters.lox

import java.util.Stack

class Resolver(val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
  private enum class FunctionType {
    NONE,
    FUNCTION,
    LAMBDA,
    METHOD,
    INITIALIZER,
    STATIC_METHOD,
  }

  private enum class ClassType {
    NONE,
    CLASS,
    SUBCLASS
  }

  private class Variable(val token: Token, val slot: Int) {
    enum class Usage {
      DECLARED,
      DEFINED,
      USED
    }

    constructor (token: Token, slot: Int, usage: Usage) : this(token, slot)  {
      this.usage = usage
    }
    var usage: Usage = Usage.DECLARED
  }

  private val scopes = Stack<MutableMap<String, Variable>>()
  private var currentFunction = FunctionType.NONE
  private var currentClass = ClassType.NONE
  private var inLoop = false

  fun resolve(statements: List<Stmt?>) = statements.forEach { resolve(it) }

  override fun visitBlockStmt(stmt: Stmt.Block) = inScope { resolve(stmt.statements) }

  override fun visitClassStmt(stmt: Stmt.Class) {
    inClass(ClassType.CLASS) {
      declare(stmt.name)
      use(stmt.name)

      val resolveClassBody: () -> Unit = {
        inScope {
          scopes.peek()["this"] = Variable(stmt.name, 0, Variable.Usage.USED)
          for (method in stmt.methods) {
            val type = if (method.name.lexeme == "init") FunctionType.INITIALIZER else FunctionType.METHOD
            resolveFunction(method, type)
          }
          for (method in stmt.staticMethods) {
            resolveFunction(method, FunctionType.STATIC_METHOD)
          }
      }
    }

      stmt.superclass?.let {
        if (it.name.lexeme == stmt.name.lexeme) {
          Lox.error(it.name, "A class cannot inherit from itself.")
        }
        resolve(it)

        inClass(ClassType.SUBCLASS) {
          inScope {
            scopes.peek()["super"] = Variable(it.name, 0, Variable.Usage.USED)
            resolveClassBody()
          }
        }
      } ?: resolveClassBody()
    }
  }

  override fun visitVarStmt(stmt: Stmt.Var) {
    declare(stmt.name)
    stmt.initializer?.let { resolve(it) }
    define(stmt.name)
  }

  override fun visitVariableExpr(expr: Expr.Variable) {
    if (scopes.isNotEmpty() && scopes.peek()[expr.name.lexeme]?.usage == Variable.Usage.DECLARED) {
      Lox.error(expr.name, "Can't read local variable in its own initializer.")
    }

    resolveLocal(expr, expr.name)
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
      scopes[idx][name.lexeme]?.let {
        interpreter.resolve(expr, scopes.size - 1 - idx, it.slot)
        if (expr !is Expr.Assign) it.usage = Variable.Usage.USED
        return
      }
    }
  }

  private fun resolveFunction(func: Stmt.Function, type: FunctionType) {
    inFunction(type) {
      inScope {
        func.params?.forEach { param ->
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
    stmt.expression?.let {
      if (currentFunction == FunctionType.INITIALIZER) {
        Lox.error(stmt.keyword, "Can't return a value from an initializer.")
      }
      resolve(it)
    } ?: Unit
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
  override fun visitGetExpr(expr: Expr.Get) = resolve(expr.instance)

  override fun visitGroupingExpr(expr: Expr.Grouping) = resolve(expr.expression)
  override fun visitLiteralExpr(expr: Expr.Literal) = Unit
  override fun visitSetExpr(expr: Expr.Set) {
    resolve(expr.value)
    resolve(expr.instance)
  }

  override fun visitSuperExpr(expr: Expr.Super) {
    when (currentClass) {
      ClassType.NONE -> Lox.error(expr.keyword, "Cannot use 'super' outside of a class.")
      ClassType.CLASS -> Lox.error(expr.keyword, "Cannot use 'super' in a class with no superclass.")
      else -> Unit
    }
    resolveLocal(expr, expr.keyword)
  }

  override fun visitThisExpr(expr: Expr.This) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword, "Cannot use 'this' outside of a class.")
      return
    }
    resolveLocal(expr, expr.keyword)
  }

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
    scope[name.lexeme] = Variable(name, scope.size)
  }

  private fun define(name: Token) {
    if (scopes.isEmpty()) return
    scopes.peek()[name.lexeme]?.usage = Variable.Usage.DEFINED
  }

  private fun use(name: Token) {
    if (scopes.isEmpty()) return
    scopes.peek()[name.lexeme]?.usage = Variable.Usage.USED
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

  private fun <R> inClass(type: ClassType, callable: () -> R): R {
    val enclosingClass = currentClass
    currentClass = type
    val retVal = callable()
    currentClass = enclosingClass

    return retVal
  }

  private fun beginScope() {
    scopes.push(HashMap())
  }
  private fun endScope() {
    scopes.peek().filter { (_, variable) -> variable.usage == Variable.Usage.DEFINED }
        .forEach { (_, variable) ->
          Lox.warning(variable.token, "Unused local variable")
        }
    scopes.pop()
  }
}