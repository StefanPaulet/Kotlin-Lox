package com.craftinginterpreters.lox

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox {
  companion object {
    fun runFile(path: String) {
      val bytes = Files.readAllBytes(Paths.get(path))
      run(String(bytes, Charset.defaultCharset()))
      takeIf { hadError }?.run {exitProcess(65)}
      takeIf { hadRuntimeError }?.run {exitProcess(75)}
    }

    fun runPrompt() {
      while(true) {
        print("> ")
        val line = readlnOrNull() ?: break
        run(line)
        hadError = false
      }
    }

    private fun run(source: String) {
      val scanner = Scanner(source)
      val tokens = scanner.scanTokens()
      val parser = Parser(tokens)
      val stmts = parser.parse()

      if (hadError) return
      interpreter.interpret(stmts)

    }

    private fun runInRepl(source: String) {
      val scanner = Scanner(source)
      val tokens = scanner.scanTokens()
      val parser = Parser(tokens)
      val stmts = parser.parse()

      if (hadError) return
      interpreter.interpret(stmts)

    }

    internal fun error(line: Int, message: String) {
      report(line, "", message)
    }

    internal fun runtimeError(error: RuntimeError) {
      System.err.println("$error \n[line ${error.token.line}]")
      hadRuntimeError = true
    }

    fun error(token: Token, message: String) {
      if (token.type == TokenType.EOF) {
        report(token.line, " at end", message);
      } else {
        report(token.line, " at '" + token.lexeme + "'", message);
      }
    }

    fun report(line: Int, where: String, message: String) {
      println("[line $line] Error $where: $message")
      hadError = true
    }

    val interpreter = Interpreter()
    var hadError = false
    var hadRuntimeError = false
  }
}

fun main(args: Array<String>) {
  if (args.size > 1) {
    println("Usage: jlox [script]");
    exitProcess(64);
  }
  if (args.size == 1) {
    Lox.runFile(args[0]);
  } else {
    Lox.runPrompt();
  }
}