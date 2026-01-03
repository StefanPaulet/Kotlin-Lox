package com.craftinginterpreters.lox

class InternalCompilerError(val reason: String) : RuntimeException("Internal compiler error: $reason")