package com.craftinginterpreters.lox

open class ControlFlowException : RuntimeException(null, null, false, false) {
  class Break : ControlFlowException() {}
  class Return(val value: Any?) : ControlFlowException() {}
}
