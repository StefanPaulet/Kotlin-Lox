package com.craftinginterpreters.lox

class Scanner(val source: String) {
  private val tokens = ArrayList<Token>()
  private var start = 0
  private var current = 0
  private var line = 1

  fun scanTokens(): List<Token> {
    while (!isAtEnd()) {
      start = current
      scanToken()
    }
    tokens.add(Token(TokenType.EOF, "", null, line))
    return tokens
  }


  private fun scanToken() {
    when (val c = advance()) {
      '(', ')', '{', '}', ',', '.', '-', '+', ';', '*', ':', '?' -> {
        symbolTokenMap[c]?.run { addToken(this) } ?: assert(false)
      }
      '!', '=', '<', '>' -> {
        tentativeEqualComposedSymbolMap[c]?.let { (eqToken, token) ->
          if (peek() == '=') {
            advance()
            addToken(eqToken)
          } else {
            addToken(token)
          }
        } ?: assert(false)
      }
      '/' -> slash()
      '"' -> string()
      ' ', '\t', '\r' -> Unit
      '\n' -> line++
      else -> {
        if (isDigit(c)) {
          number()
        } else if (isAlpha(c)) {
          identifier()
        } else {
          Lox.error(line, "Unexpected character.")
        }
      }
    }
  }

  private fun isAtEnd(): Boolean = current >= source.length

  private fun advance(): Char = source[current++]

  private fun addToken(type: TokenType) = addToken(type, null)

  private fun peek() = if (isAtEnd()) '\u0000' else source[current]

  private fun peekNext() = if  (current + 1 >= source.length) '\u0000' else source[current + 1]

  private fun isDigit(c: Char) = c in '0'..'9'
  private fun isAlpha(c: Char) = c in 'a' .. 'z' || c in 'A' .. 'Z' || c == '_'
  private fun isAlphaNumeric(c: Char) = isDigit(c) || c in 'a' .. 'z' || c in 'A' .. 'Z' || c == '_'

  private fun slash() {
    if (match('/')) {
      consumeLineComment()
      return
    }
    if (match('*')) {
      consumeMultiLineComment()
      return
    }
    addToken(TokenType.SLASH)
  }

  private fun consumeLineComment() {
    while (peek() != '\n' && !isAtEnd()) advance()
  }

  private fun consumeMultiLineComment() {
    while (!(match('*') && match('/')) && !isAtEnd()) {
      if (source[current] == '\n') {
        line++
      }
      advance()
    }
  }

  private fun string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() != '\n') line++
      advance()
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.")
      return
    }

    advance()
    addToken(TokenType.STRING, source.substring(start + 1, current - 1))
  }

  private fun number() {
    while (isDigit(peek())) advance()

    if (peek() == '.' && isDigit(peekNext())) {
      advance()

      while (isDigit(peek())) advance()
    }
    addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
  }

  private fun identifier() {
    while (isAlphaNumeric(peek())) advance()
    keyWords[source.substring(start, current)]
        ?.let { addToken(it) }
        ?:run { addToken(TokenType.IDENTIFIER)}
  }

  private fun addToken(type: TokenType, literal: Any?) {
    val text = source.substring(start, current)
    tokens.add(Token(type, text, literal, line))
  }

  private fun match(expected: Char): Boolean {
    current = current
        .takeIf { !isAtEnd() && source[it] == expected }
        ?. run { this + 1 }
        ?: return false
    return true
  }

  companion object {
    val keyWords = mapOf(
      "and" to TokenType.AND,
      "class" to TokenType.CLASS,
      "else" to TokenType.ELSE,
      "false" to TokenType.FALSE,
      "for" to TokenType.FOR,
      "fun" to TokenType.FUN,
      "if" to TokenType.IF,
      "nil" to TokenType.NIL,
      "or" to TokenType.OR,
      "print" to TokenType.PRINT,
      "return" to TokenType.RETURN,
      "super" to TokenType.SUPER,
      "this" to TokenType.THIS,
      "true" to TokenType.TRUE,
      "var" to TokenType.VAR,
      "while" to TokenType.WHILE,
      "break" to TokenType.BREAK
    )

    val symbolTokenMap = mapOf(
      '(' to TokenType.LEFT_PAREN,
      ')' to TokenType.RIGHT_PAREN,
      '{' to TokenType.LEFT_BRACE,
      '}' to TokenType.RIGHT_BRACE,
      ',' to TokenType.COMMA,
      '.' to TokenType.DOT,
      '-' to TokenType.MINUS,
      '+' to TokenType.PLUS,
      ';' to TokenType.SEMICOLON,
      '*' to TokenType.STAR,
      ':' to TokenType.COLON,
      '?' to TokenType.QUERY
    )

    val tentativeEqualComposedSymbolMap = mapOf(
      '!' to Pair(TokenType.BANG_EQUAL, TokenType.BANG),
      '=' to Pair(TokenType.EQUAL_EQUAL, TokenType.EQUAL),
      '<' to Pair(TokenType.LESS_EQUAL, TokenType.LESS),
      '>' to Pair(TokenType.GREATER_EQUAL, TokenType.GREATER),
    )
  }
}