package com.craftinginterpreters.lox

class Parser(val tokens: List<Token>) {
  private var current = 0
  private var inLoop = false

  private class ParserError: RuntimeException()

  fun parse(): List<Stmt?> {
    val stmtList = ArrayList<Stmt?>()
    while (!isAtEnd()) {
      stmtList.add(declaration())
    }
    return stmtList
  }

  fun parseExpression(): Expr? {
    return try {
      expression()
    } catch (_: ParserError) {
      null
    }
  }

  private fun declaration(): Stmt? {
    return try {
      if (match(TokenType.FUN)) return funDeclaration("function")
      if (match(TokenType.VAR)) return varDeclaration()
      statement()
    } catch (_: ParserError) {
      synchronize()
      null
    }
  }

  private fun funDeclaration(kind: String): Stmt {
    val name = consume(TokenType.IDENTIFIER, "Expected $kind name.")
    consume(TokenType.LEFT_PAREN, "Expected '(' after $kind name.")
    val parameters = mutableListOf<Token>()
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (parameters.size >= 255) {
          error(peek(), "Can't have more than 255 parameters.")
        }
        parameters.add(consume(TokenType.IDENTIFIER, "Expected parameter name."))
      } while(match(TokenType.COMMA))
    }
    consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters of function.")

    consume(TokenType.LEFT_BRACE, "Expected '{' before $kind body.")
    val body = block()
    return Stmt.Function(name, parameters, body)
  }

  private fun varDeclaration(): Stmt {
    val name = consume(TokenType.IDENTIFIER, "Expected variable name.")
    val initializer = if (match(TokenType.EQUAL)) expression() else null
    consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.")
    return Stmt.Var(name, initializer)
  }

  private fun statement(): Stmt {
    if (match(TokenType.FOR)) return forStatement()
    if (match(TokenType.IF)) return ifStatement()
    if (match(TokenType.PRINT)) return printStatement()
    if (match(TokenType.RETURN)) return returnStatement()
    if (match(TokenType.WHILE)) return whileStatement()
    if (match(TokenType.BREAK)) return breakStatement()
    if (match(TokenType.LEFT_BRACE)) return Stmt.Block(block())
    return expressionStatement()
  }

  private fun forStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expected '(' after 'for'.")
    val initializer = if (match(TokenType.SEMICOLON)) null else
      if (match(TokenType.VAR)) varDeclaration() else
      expressionStatement()

    val condition = if (!check(TokenType.SEMICOLON)) expression() else null
    consume(TokenType.SEMICOLON, "Expected ';' after loop condition.")

    val increment = if(!check(TokenType.RIGHT_PAREN)) expression() else null
    consume(TokenType.RIGHT_PAREN, "Expected ')' after for clause.")

    var body = doInLoop { statement() }

    body = increment?.let { inc -> Stmt.Block(listOf(body, Stmt.Expression(inc)))} ?: body
    body = condition?.let { cond -> Stmt.While(cond,  body) } ?: body
    body = initializer?.let { init -> Stmt.Block(listOf(init, body))} ?: body
    return body
  }

  private fun ifStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'.")
    val condition = expression()
    consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition.")

    val thenBranch = statement()
    val elseBranch = if (match(TokenType.ELSE)) statement() else null

    return Stmt.If(condition, thenBranch, elseBranch)
  }

  private fun printStatement(): Stmt {
    val expression = expression()
    consume(TokenType.SEMICOLON, "Expected ';' after value.")
    return Stmt.Print(expression)
  }

  private fun returnStatement(): Stmt {
    val keyword = previous()
    val value = if(!check(TokenType.SEMICOLON)) expression() else null
    consume(TokenType.SEMICOLON, "Expected ';' after return value.")

    return Stmt.Return(keyword, value)
  }

  private fun whileStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expected '(' after 'while'.")
    val condition = expression()
    consume(TokenType.RIGHT_PAREN, "Expected ')' after while condition.")

    val body = doInLoop { statement() }

    return Stmt.While(condition, body)
  }

  private fun breakStatement(): Stmt {
    val stmt = Stmt.Break(previous())
    if (!inLoop) {
      throw error(stmt.keyword, "Break statement may not appear outside of a loop.")
    }
    consume(TokenType.SEMICOLON, "Expected ';' after break.")
    return stmt
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
    if (match(TokenType.FUN)) return lambda()

    var expr = assignment()
    while (match(TokenType.COMMA)) {
      val comma = previous()
      val right = assignment()
      expr = Expr.Binary(expr, comma, right)
    }

    return expr
  }

  private fun assignment(): Expr {
    val expr = or()
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

  private fun or(): Expr {
    var expr = and()
    while (match(TokenType.OR)) {
      val operator = previous()
      val right = and()
      expr = Expr.Logical(expr, operator, right)
    }
    return expr
  }

  private fun and(): Expr {
    var expr = ternary()
    while (match(TokenType.AND)) {
      val operator = previous()
      val right = ternary()
      expr = Expr.Logical(expr, operator, right)
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
    return call()
  }

  private fun call(): Expr {
    var expr = primary()
    while (true) {
      if (match(TokenType.LEFT_PAREN)) {
        expr = finishCall(expr)
      } else {
        break
      }
    }
    return expr
  }

  private fun finishCall(expr: Expr): Expr {
    val arguments = mutableListOf<Expr>()
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (arguments.size >= 255) { error(peek(), "Can't have more than 255 arguments.") }
        arguments.add(assignment())
      } while (match(TokenType.COMMA))
    }
    val paren = consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments of function call.")
    return Expr.Call(expr, paren, arguments)
  }

  private fun primary(): Expr {
    if (match(TokenType.FALSE)) return Expr.Literal(false)
    if (match(TokenType.TRUE)) return Expr.Literal(true)
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

  private fun lambda(): Expr {
    val head = previous()
    consume(TokenType.LEFT_PAREN, "Expected '(' after lambda name.")
    val parameters = mutableListOf<Token>()
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (parameters.size >= 255) {
          error(peek(), "Can't have more than 255 parameters.")
        }
        parameters.add(consume(TokenType.IDENTIFIER, "Expected parameter name."))
      } while(match(TokenType.COMMA))
    }
    consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters of function.")

    consume(TokenType.LEFT_BRACE, "Expected '{' before lambda body.")
    val body = block()
    return Expr.Lambda(head, parameters, body)
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

  private fun <R> doInLoop(callable: () -> R): R {
    inLoop = true
    val result = callable()
    inLoop = false
    return result
  }

  private fun check(type: TokenType) = !isAtEnd() && peek().type == type

  private fun advance(): Token {
    if (!isAtEnd()) {
      current++
    }
    return previous()
  }

  private fun isAtEnd(): Boolean = peek().type == TokenType.EOF

  private fun peek(): Token = tokens[current]

  private fun previous(): Token = tokens[current - 1]
}