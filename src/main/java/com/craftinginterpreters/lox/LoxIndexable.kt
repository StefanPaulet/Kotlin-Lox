package com.craftinginterpreters.lox

interface LoxIndexable {
  operator fun get(index: Int): Any?
  operator fun set(index: Int, value: Any?)
}