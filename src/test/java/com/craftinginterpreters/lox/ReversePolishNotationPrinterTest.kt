package com.craftinginterpreters.lox

import org.junit.jupiter.api.Assertions.*

class ReversePolishNotationPrinterTest {

  @org.junit.jupiter.api.Test
  fun print() {
    val result = ReversePolishNotationPrinter().print(
      Expr.Binary(
        Expr.Grouping(
          Expr.Binary(
            Expr.Literal(1),
            Token(TokenType.PLUS, "+", null, 1),
            Expr.Literal(2)
          )
        ),
        Token(TokenType.OR, "*", null, 1),
        Expr.Grouping(
          Expr.Binary(
            Expr.Literal(4),
            Token(TokenType.MINUS, "-", null, 1),
            Expr.Literal(3)
          )
        ),
      )
    )
    assertEquals(result, "1 2 + 4 3 - *")
  }
}