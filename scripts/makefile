# Makefile for building a single directory of Java source files. It requires
# a DIR variable to be set.

BUILD_DIR := output
JAR := $(BUILD_DIR)/jlox.jar

PACKAGE := lox
SRC_DIR := ../src/main/java/com/craftinginterpreters/$(PACKAGE)
SOURCES := $(wildcard $(SRC_DIR)/*.kt)

KOTLIN_OPTIONS := -Werror

.PHONY: default clean

default: $(JAR)

$(JAR): $(SOURCES)
	@mkdir -p $(BUILD_DIR)
	@kotlinc $(SOURCES) $(KOTLIN_OPTIONS) -include-runtime -d $(JAR)
	@printf "%8s %-60s %s\n" kotlinc "$(SOURCES)" "$(KOTLIN_OPTIONS)"

clean:
	@rm -rf $(BUILD_DIR)
	