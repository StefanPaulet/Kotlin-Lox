//this file was automatically generated using the tool/GenerateAst.kt file
package com.craftinginterpreters.lox

abstract class Stmt {
	abstract fun <R> accept(visitor: Visitor<R>): R
	class Expression(val expression: Expr, ) : Stmt() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitExpressionStmt(this)
    }
	}
	class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?, ) : Stmt() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitIfStmt(this)
    }
	}
	class Print(val expression: Expr, ) : Stmt() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitPrintStmt(this)
    }
	}
	class While(val condition: Expr, val body: Stmt, ) : Stmt() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitWhileStmt(this)
    }
	}
	class Var(val name: Token, val initializer: Expr?, ) : Stmt() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitVarStmt(this)
    }
	}
	class Block(val statements: List<Stmt?>, ) : Stmt() {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitBlockStmt(this)
    }
	}
	interface Visitor<R> {
		fun visitExpressionStmt(stmt: Expression): R
		fun visitIfStmt(stmt: If): R
		fun visitPrintStmt(stmt: Print): R
		fun visitWhileStmt(stmt: While): R
		fun visitVarStmt(stmt: Var): R
		fun visitBlockStmt(stmt: Block): R
	}
}
