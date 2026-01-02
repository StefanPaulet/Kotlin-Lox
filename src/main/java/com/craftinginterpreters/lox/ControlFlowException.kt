package com.craftinginterpreters.lox

open class ControlFlowException : Throwable() {
  class BreakException : ControlFlowException() {}
}
