package com.craftinginterpreters.lox

class Parser(val tokens: List<Token>) {
  private var current = 0

  private class ParserError: RuntimeException()

  fun parse(): List<Stmt?> {
    val stmtList = ArrayList<Stmt?>()
    while (!isAtEnd()) {
      stmtList.add(declaration())
    }
    return stmtList
  }

  private fun declaration(): Stmt? {
    try {
      if (match(TokenType.VAR)) return varDeclaration()
      return statement()
    } catch (error: ParserError) {
      synchronize()
      return null
    }
  }

  private fun varDeclaration(): Stmt {
    val name = consume(TokenType.IDENTIFIER, "Expected variable name.")
    val initializer = if (match(TokenType.EQUAL)) expression() else null
    consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.")
    return Stmt.Var(name, initializer)
  }

  private fun statement(): Stmt {
    if (match(TokenType.PRINT)) return printStatement()
    if (match(TokenType.LEFT_BRACE)) return Stmt.Block(block())
    return expressionStatement()
  }

  private fun printStatement(): Stmt {
    val expression = expression()
    consume(TokenType.SEMICOLON, "Expected ';' after value.")
    return Stmt.Print(expression)
  }

  private fun block(): List<Stmt?> {
    val stmtList = mutableListOf<Stmt?>()
    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      stmtList.add(declaration())
    }
    consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
    return stmtList
  }

  private fun expressionStatement(): Stmt {
    val expression = expression()
    consume(TokenType.SEMICOLON, "Expected ';' after expression.")
    return Stmt.Expression(expression)
  }

  private fun expression(): Expr {
    var expr = assignment()
    while (match(TokenType.COMMA)) {
      val comma = previous()
      val right = assignment()
      expr = Expr.Binary(expr, comma, right)
    }

    return expr
  }

  private fun assignment(): Expr {
    val expr = ternary()
    if (match(TokenType.EQUAL)) {
      val equals = previous()
      val value = assignment()

      if (expr is Expr.Variable) {
        return Expr.Assign(expr.name, value)
      }
      error(equals, "Invalid assignment target")
    }

    return expr
  }

  private fun ternary(): Expr {
    var expr = equality()
    if (match(TokenType.QUERY)) {
      val ifTrue = expression()
      consume(TokenType.COLON, "Found '?' operator without matching ':'")
      val ifFalse = expression()
      expr = Expr.Ternary(expr, ifTrue, ifFalse)
    }
    return expr
  }

  private fun equality(): Expr {
    var expr = comparison()

    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      val operator = previous()
      val right = comparison()

      expr = Expr.Binary(expr, operator, right)
    }
    return expr
  }


  private fun comparison(): Expr {
    var expr = term()

    while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
      val operator = previous()
      val right = term()
      expr = Expr.Binary(expr, operator, right)
    }
    return expr
  }

  private fun term(): Expr {
    var expr: Expr = if (match(TokenType.PLUS)) {
      val operator = previous()
      error(operator, "Missing left-hand side operator")
      Expr.Binary(Expr.Literal(null),  operator, factor())
    } else {
      factor()
    }

    while (match(TokenType.MINUS, TokenType.PLUS)) {
      val operator = previous()
      val right = factor()
      expr = Expr.Binary(expr, operator, right)
    }
    return expr
  }

  private fun factor(): Expr {

    var expr = if (match(TokenType.SLASH, TokenType.STAR)) {
      val operator = previous()
      error(operator, "Missing left-hand side operator")
      Expr.Binary(Expr.Literal(null),  operator,unary())
    } else {
      unary()
    }

    while (match(TokenType.SLASH, TokenType.STAR)) {
      val operator = previous()
      val right = unary()
      expr = Expr.Binary(expr, operator, right)
    }
    return expr
  }

  private fun unary(): Expr {
    if (match(TokenType.BANG, TokenType.MINUS)) {
      val operator = previous()
      val right = unary()
      return Expr.Unary(operator, right)
    }
    return primary()
  }

  private fun primary(): Expr {
    if (match(TokenType.FALSE)) return Expr.Literal(false as Object)
    if (match(TokenType.TRUE)) return Expr.Literal(true as Object)
    if (match(TokenType.NIL)) return Expr.Literal(null)
    if (match(TokenType.NUMBER, TokenType.STRING)) return Expr.Literal(previous().literal)
    if (match(TokenType.LEFT_PAREN)) {
      val expr = expression()
      consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.")
      return Expr.Grouping(expr)
    }
    if (match(TokenType.IDENTIFIER)) {
      return Expr.Variable(previous())
    }

    throw error(peek(), "Token cannot represent primary expression.")
  }

  private fun synchronize() {
    advance()

    while (!isAtEnd()) {
      if (previous().type == TokenType.SEMICOLON) return

      when (peek().type) {
        TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF,
           TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> return
        else -> Unit
      }

      advance()
    }
  }

  private fun consume(type: TokenType, message: String): Token {
    if (check(type)) return advance()
    throw error(peek(), message)
  }

  private fun error(token: Token, message: String): ParserError {
    Lox.error(token, message)
    return ParserError()
  }

  private fun match(vararg types: TokenType): Boolean {
    for (type in types) {
      if (check(type)) {
        advance()
        return true
      }
    }

    return false
  }

  private fun check(type: TokenType) = !isAtEnd() && peek().type == type

  private fun advance(): Token {
    if (!isAtEnd()) {
      current++;
    }
    return previous()
  }

  private fun isAtEnd(): Boolean = peek().type == TokenType.EOF

  private fun peek(): Token = tokens[current]

  private fun previous(): Token = tokens[current - 1]
}