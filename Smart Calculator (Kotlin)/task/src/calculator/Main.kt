package calculator

import java.math.BigInteger
import java.util.*

/*
Infix to postfix expression using Stack (2 + 3 infix becomes 2 3 + postfix).
https://www.free-online-calculator-use.com/infix-to-postfix-converter.html
https://brilliant.org/wiki/shunting-yard-algorithm/

Terminology:
Operand = the object of a mathematical operation
Token =
Operator:
        Operator	Symbol	Precedence	Associativity
        Exponent	^	4	Right to Left
        Multiplication	*	3	Left to Right
        Division	/	3	Left to Right
        Addition	+	2	Left to Right
        Subtraction	-	2	Left to Right
 */

class Calculator {
    private val variables: MutableMap<String, BigInteger> = mutableMapOf()

    fun init() {
        while (true) {
            val input = readln()
            if (input.isEmpty()) continue

            // If the input is a command
            if (input.startsWith("/")) {
                when (input) {
                    "/exit" -> break
                    "/help" -> println("The program calculates the sum of numbers")
                    else -> println("Unknown command.")
                }
            } else processInput(input)
        }
        println("Bye!")
    }

    // Create variable or transform the infix input to postfix and then evaluate it
    private fun processInput(input: String) {
        try {
            if (input.contains("=")) {
                createVariable(input)
            } else {
                val queue = infixToPostfix(input)
                val result = evaluatePostfix(queue)
                println(result)
            }
        } catch (e: UnknownVariableException) {
            println(e.message)
        } catch (e: InvalidExpressionException) {
            println(e.message)
        } catch (e: InvalidIdentifierException) {
            println(e.message)
        }
    }

    // Get the variable decleration and value it to the variables map as k, v
    private fun createVariable(input: String) {
        val variable = input.substringBefore("=").trim()
        if (!hasValidVariableName(variable)) {
            println("Invalid identifier")
            return
        }

        val value = input.substringAfter("=").trim()
        if (!isNumber(value) && !hasValidVariableName(value)) {
            println("Invalid identifier")
            return
        }
        variables[variable] = value.toBigIntegerOrNull() ?: variables[value] ?: throw UnknownVariableException()
    }

    private fun hasValidVariableName(input: String) = !Regex("[^a-zA-Z]").containsMatchIn(input)
    private fun isNumber(input: String) = Regex("-?\\d+").matches(input)

    private fun infixToPostfix(infix: String): String {
        if (!isBalancedParentheses(infix)) throw InvalidExpressionException()

        val stack = Stack<Char>()
        val postfixExpression = mutableListOf<String>()
        val operatorsPrecedence = mapOf('+' to 1, '-' to 1, '*' to 2, '/' to 2)

        //println("Infix: $infix")
        val tokens = tokenize(infix)
        //println("Tokenized tokens: $tokens")

        for (token in tokens) {
            val firstChar = token.first()
            when {
                // Token is a numeric operand, add it to the postfix expression
                firstChar.isDigit() -> postfixExpression.add(token)
                // Token is an open parenthesis, push it onto the stack
                firstChar == '(' -> stack.push(firstChar)
                // Token is a close parenthesis
                // Pop operators from the stack and add them to the postfix expression
                firstChar == ')' -> {
                    while (stack.isNotEmpty() && stack.peek() != '(') {
                        postfixExpression.add(stack.pop().toString())
                    }
                    if (stack.isNotEmpty() && stack.peek() == '(') {
                        stack.pop() // Pop and discard the open parenthesis
                    }
                }

                // Token is an operator
                "+-*/".contains(firstChar) -> {
                    while (stack.isNotEmpty() && stack.peek() != '(' &&
                        (operatorsPrecedence.getOrDefault(
                            stack.peek(),
                            0
                        ) >= operatorsPrecedence.getOrDefault(firstChar, 0) ||
                                (stack.peek() == '-' && firstChar == '-'))
                    ) {
                        // Pop operators from the stack and add them to the postfix expression
                        postfixExpression.add(stack.pop().toString())
                    }
                    // Push the current operator onto the stack
                    stack.push(firstChar)
                }
                // Token is a variable, get its value and add it to the postfix expression
                variables.containsKey(token) -> {
                    val variable = variables[token] ?: throw UnknownVariableException()
                    postfixExpression.add(variable.toString())
                }
            }
        }
        // Pop any remaining operators from the stack and add them to the postfix expression
        while (stack.isNotEmpty()) {
            postfixExpression.add(stack.pop().toString())
        }

        return postfixExpression.joinToString(" ")
    }

    private fun evaluatePostfix(postfix: String): BigInteger {
        val stack = Stack<BigInteger>()
        val tokens = postfix.split(" ")
        //println("Postfix: $postfix")
        for (token in tokens) {
            if (token.isEmpty()) {
                continue // Skip empty tokens
            }
            if (token.matches(Regex("-?\\d+(\\.\\d+)?"))) {
                // Token is an operand, push it onto the stack
                stack.push(token.toBigInteger())
            } else {
                // Token is an operator
                if (stack.size < 2) {
                    // There must be at least two operands on the stack for a binary operator
                    throw IllegalArgumentException("Invalid postfix expression")
                }

                // Pop the top two operands from the stack
                val operand2 = stack.pop()
                val operand1 = stack.pop()

                // Apply the operator and push the result back onto the stack
                val result = when (token) {
                    "+" -> operand1 + operand2
                    "-" -> operand1 - operand2
                    "*" -> operand1 * operand2
                    "/" -> operand1 / operand2
                    else -> throw InvalidExpressionException()
                }
                // The final result is the only value left on the stack
                stack.push(result)
            }
        }

        // Return result
        if (stack.size == 1) {
            return stack.pop()
        } else {
            throw UnknownVariableException()
        }
    }
}

private fun isBalancedParentheses(expression: String): Boolean {
    val stack = Stack<Char>()

    for (char in expression) {
        if (char == '(') {
            stack.push(char)
        } else if (char == ')') {
            if (stack.isEmpty() || stack.pop() != '(') {
                return false
            }
        }
    }

    return stack.isEmpty()
}

private fun tokenize(line: String): MutableList<String> {
    // https://stackoverflow.com/questions/3373885/splitting-a-simple-maths-expression-with-regex
    val regex = "(?<=op)|(?=op)".replace("op", "[-+*/()^]")

    // super ugly :-(
    return simplifyOperators(line)
        .trim()
        .split(regex.toRegex())
        .filter { it.trim().isNotEmpty() }
        .map { it.trim() }
        .toMutableList()
}

private fun simplifyOperators(expr: String): String {
    if (expr.contains("**")) {
        throw InvalidExpressionException()
    }
    if (expr.contains("//")) {
        throw InvalidExpressionException()
    }
    var modifiedExpr = expr
    var prevExpr: String

    do {
        prevExpr = modifiedExpr
        modifiedExpr = modifiedExpr
            .replace("++", "+")
            .replace("--", "+")
            .replace("+-", "-")
            .replace("-+", "-")
    } while (modifiedExpr != prevExpr)
    return modifiedExpr
}

class UnknownVariableException : Exception("Unknown variable")
class InvalidExpressionException : Exception("Invalid expression")
class InvalidIdentifierException : Exception("Invalid identifier")

fun main() {
    val calculator = Calculator()
    calculator.init()
}



