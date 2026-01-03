package com.craftinginterpreters.lox


class Logger {
  var silent = false

  enum class Severity {
    LOG,
    WARNING,
    ERROR,
    RUNTIME_ERROR
  }

  internal fun warning(token: Token, message: String) {
    report(token.line, " at '" + token.lexeme + "'", message, Severity.WARNING)
  }

  internal fun error(token: Token, message: String) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message, Severity.ERROR)
    } else {
      report(token.line, " at '" + token.lexeme + "'", message, Severity.ERROR)
    }
  }

  internal fun error(line: Int, message: String) {
    report(line, "", message, Severity.ERROR)
  }

  internal fun runtimeError(error: RuntimeError) {
    report(error.token.line, "", error.toString(), Severity.RUNTIME_ERROR)
  }

  private fun report(line: Int, where: String, message: String, severity: Severity) {
    if (silent) return
    when (severity) {
      Severity.LOG -> println("[line $line] $where: $message")
      Severity.WARNING -> println("[line $line] Warning $where: $message")
      Severity.ERROR -> println("[line $line] Error $where: $message")
      Severity.RUNTIME_ERROR -> System.err.println("$message \n[line ${line}]")
    }
  }
}