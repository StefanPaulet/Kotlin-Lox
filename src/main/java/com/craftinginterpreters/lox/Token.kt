package com.craftinginterpreters.lox

class Token(val type: TokenType, val lexeme: String, val literal: Object?, val line: Int) {
  override fun toString(): String {
    return "$type $lexeme $literal"
  }
}