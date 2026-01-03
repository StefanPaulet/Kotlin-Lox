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
        runInRepl(line)
        hadError = false
      }
    }

    private fun run(source: String) {
      val scanner = Scanner(source)
      val tokens = scanner.scanTokens()
      val parser = Parser(tokens)
      val stmts = parser.parse()

      takeUnless { hadError }?.let {
        val resolver = Resolver(interpreter)
        resolver.resolve(stmts)
      }?.takeUnless { hadError }?.let {
        interpreter.interpret(stmts)
      }
    }

    private fun runInRepl(source: String) {
      logger.silent = true

      val scanner = Scanner(source)
      val tokens = scanner.scanTokens()
      val parser = Parser(tokens)
      val stmts = parser.parse()

      takeUnless { hadError }?.let{
        val resolver = Resolver(interpreter)
        resolver.resolve(stmts)
      }?.takeUnless { hadError }?.let{
          logger.silent = false
          interpreter.interpret(stmts)
      }?:run {
          hadError = false
          val parser = Parser(tokens)
          val expression = parser.parseExpression()
          expression?.takeIf { !hadError }
              ?.let { interpreter.interpret(expression) }
              ?.takeIf { !hadRuntimeError }
              ?.let { println(it) }
        }
    }

    internal fun error(line: Int, message: String) {
      logger.error(line, message)
      hadError = true
    }

    fun error(token: Token, message: String) {
      logger.error(token, message)
      hadError = true
    }

    internal fun runtimeError(error: RuntimeError) {
      logger.runtimeError(error)
      hadRuntimeError = true
    }

    val interpreter = Interpreter()
    var hadError = false
    var hadRuntimeError = false
    val logger = Logger()
  }
}

fun main(args: Array<String>) {
  if (args.size > 1) {
    println("Usage: jlox [script]")
    exitProcess(64)
  }
  if (args.size == 1) {
    Lox.runFile(args[0])
  } else {
    Lox.runPrompt()
  }
}