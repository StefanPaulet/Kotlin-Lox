package com.craftinginterpreters.lox

class ReversePolishNotationPrinter : Expr.Visitor<String> {
  fun print(expr: Expr): String {
    return expr.accept(this).trim()
  }

  override fun visitBinaryExpr(expr: Expr.Binary): String {
    return polish(expr.operator.lexeme, expr.left, expr.right)
  }

  override fun visitGroupingExpr(expr: Expr.Grouping): String {
    return expr.expression.accept(this)
  }

  override fun visitLiteralExpr(expr: Expr.Literal): String {
    return expr.value?.run { this.toString() } ?: "nil"
  }

  override fun visitUnaryExpr(expr: Expr.Unary): String {
    return polish(expr.operator.lexeme, expr.right)
  }

  override fun visitTernaryExpr(expr: Expr.Ternary): String {
    return polish("?", expr.condition, expr.ifTrue, expr.ifFalse)
  }

  override fun visitVariableExpr(expr: Expr.Variable): String {
    TODO("Not yet implemented")
  }

  override fun visitAssignExpr(expr: Expr.Assign): String {
    TODO("Not yet implemented")
  }

  override fun visitLogicalExpr(expr: Expr.Logical): String {
    TODO("Not yet implemented")
  }

  override fun visitCallExpr(expr: Expr.Call): String {
    TODO("Not yet implemented")
  }

  override fun visitLambdaExpr(expr: Expr.Lambda): String {
    TODO("Not yet implemented")
  }

  override fun visitGetExpr(expr: Expr.Get): String {
    TODO("Not yet implemented")
  }

  override fun visitSetExpr(expr: Expr.Set): String {
    TODO("Not yet implemented")
  }

  override fun visitThisExpr(expr: Expr.This): String {
    TODO("Not yet implemented")
  }

  private fun polish(name: String, vararg expressions: Expr): String {
    return StringBuilder().run {
      expressions.forEach {expr -> append("${expr.accept(this@ReversePolishNotationPrinter)} ")}
      append(name)
      toString()
    }
  }
}