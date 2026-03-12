package com.craftinginterpreters.lox

private val LoxArrayClass = LoxClass("LoxArray", null, hashMapOf(), null)

class LoxArray(val data: Array<Any?>) : LoxIndexable, LoxInstance(LoxArrayClass) {
  override operator fun get(index: Int): Any? = data[index]
  override operator fun set(index: Int, value: Any?) { data[index] = value; }
  fun join(other: LoxArray): LoxArray =
      LoxArray(arrayOf(*this.data, *other.data))

  override fun get(name: Token): Any {
    when (name.lexeme) {
      "length" -> return data.size
      else -> throw RuntimeError(name, "Undefined property of array '${name.lexeme}'")
    }
  }

  override fun set(name: Token, value: Any?) {
    throw RuntimeError(name, "Cannot add properties to array")
  }

  override fun toString(): String = data.joinToString(", ", "[", "]")
}