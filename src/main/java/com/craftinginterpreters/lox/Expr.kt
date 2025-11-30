//this file was automatically generated using the tool/GenerateAst.kt file
package com.craftinginterpreters.lox

abstract class Expr {
	abstract fun <R> accept(visitor: Visitor<R>): R
	class Assign(val name: Token, val value: Expr, ) : Expr() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitAssignExpr(this)
    }
	}
	class Binary(val left: Expr, val operator: Token, val right: Expr, ) : Expr() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitBinaryExpr(this)
    }
	}
	class Grouping(val expression: Expr, ) : Expr() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitGroupingExpr(this)
    }
	}
	class Literal(val value: Object?, ) : Expr() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitLiteralExpr(this)
    }
	}
	class Unary(val operator: Token, val right: Expr, ) : Expr() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitUnaryExpr(this)
    }
	}
	class Ternary(val condition: Expr, val ifTrue: Expr, val ifFalse: Expr, ) : Expr() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitTernaryExpr(this)
    }
	}
	class Variable(val name: Token, ) : Expr() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitVariableExpr(this)
    }
	}
	interface Visitor<R> {
		fun visitAssignExpr(expr: Assign): R
		fun visitBinaryExpr(expr: Binary): R
		fun visitGroupingExpr(expr: Grouping): R
		fun visitLiteralExpr(expr: Literal): R
		fun visitUnaryExpr(expr: Unary): R
		fun visitTernaryExpr(expr: Ternary): R
		fun visitVariableExpr(expr: Variable): R
	}
}
