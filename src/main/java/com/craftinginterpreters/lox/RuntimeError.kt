package com.craftinginterpreters.lox

internal class RuntimeError(val token: Token, message: String?) : RuntimeException(message)