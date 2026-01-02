package com.craftinginterpreters.tool

import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.Locale.getDefault
import kotlin.jvm.Throws
import kotlin.system.exitProcess

class GenerateAst {
  companion object {
    @Throws(IOException::class)
    fun defineAst(output: String, baseName: String, types: Array<String>) {
      val path = "$output/com/craftinginterpreters/lox/$baseName.kt"

      File(path).printWriter().use { writer ->
        writer.println("//this file was automatically generated using the tool/GenerateAst.kt file")
        writer.println("package com.craftinginterpreters.lox")
        writer.println()
        writer.println("abstract class $baseName {")

        writer.println("\tabstract fun <R> accept(visitor: Visitor<R>): R")

        types.forEach { type ->
          val splits = type.split(":")
          val className = splits[0].trim()
          val fields = splits[1].trim()
          defineType(writer, baseName, className, fields)
        }
        defineVisitor(writer, baseName, types)
        writer.println("}")
      }
    }

    fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String?) {
      writer.print("\tclass $className(")
      fieldList?.split(", ")
          ?.forEach {
            val (type, name) = it.split(" ")
            writer.print("val $name: $type, ")
          }
      writer.println(") : $baseName() {")


      writer.println("""    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visit$className$baseName(this)
    }""")

      writer.println("\t}")
    }

    fun defineVisitor(writer: PrintWriter, baseName: String, types: Array<String>) {
      writer.println("\tinterface Visitor<R> {")
      types.forEach { type ->
        val typeName = type.split(":")[0].trim()
        writer.println("\t\tfun visit$typeName$baseName(${baseName.lowercase(getDefault())}: $typeName): R")
      }
      writer.println("\t}")
    }
  }
}

fun main(args: Array<String>) {
  if (args.size != 1) {
    println("Usage: generate_ast <output_directory>")
    exitProcess(64)
  }

  val outputDir = args[0]
  GenerateAst.defineAst(outputDir, "Expr", arrayOf(
    "Assign   : Token name, Expr value",
    "Binary   : Expr left, Token operator, Expr right",
    "Call     : Expr callee, Token paren, List<Expr> arguments",
    "Grouping : Expr expression",
    "Literal  : Any? value",
    "Logical  : Expr left, Token operator, Expr right",
    "Unary    : Token operator, Expr right",
    "Ternary  : Expr condition, Expr ifTrue, Expr ifFalse",
    "Variable : Token name"
  ))

  GenerateAst.defineAst(outputDir, "Stmt", arrayOf(
    "Expression : Expr expression",
    "Function   : Token name, List<Token> params, List<Stmt?> body",
    "If         : Expr condition, Stmt thenBranch, Stmt? elseBranch",
    "Print      : Expr expression",
    "Return     : Token keyword, Expr? expression",
    "While      : Expr condition, Stmt body",
    "Var        : Token name, Expr? initializer",
    "Block      : List<Stmt?> statements",
    "Break      : Token keyword",
  ))
}