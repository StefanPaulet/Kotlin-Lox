package com.craftinginterpreters.lox

class AstPrinter : Expr.Visitor<String> {
  fun print(expr: Expr): String {
    return expr.accept(this)
  }

  override fun visitBinaryExpr(expr: Expr.Binary): String {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right)
  }

  override fun visitGroupingExpr(expr: Expr.Grouping): String {
    return parenthesize("group", expr.expression)
  }

  override fun visitLiteralExpr(expr: Expr.Literal): String {
    return expr.value?.run { this.toString() } ?: "nil"
  }

  override fun visitUnaryExpr(expr: Expr.Unary): String {
    return parenthesize(expr.operator.lexeme, expr.right)
  }

  override fun visitTernaryExpr(expr: Expr.Ternary): String {
    return parenthesize("?:", expr.condition, expr.ifTrue, expr.ifFalse)
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

  private fun parenthesize(name: String, vararg expressions: Expr): String {
    StringBuilder().run {
      append("($name")
      expressions.forEach {expr -> append(" ${expr.accept(this@AstPrinter)}")}
      append(")")
      return toString()
    }
  }
}