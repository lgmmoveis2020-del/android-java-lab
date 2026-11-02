package com.lgm.androidlab.core.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalculatorTest {

    private final Calculator calc = new Calculator();

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = '|', value = {
            "1 + 2               | 3",
            "2 + 3 * 4           | 14",
            "(2 + 3) * 4         | 20",
            "10 / 4              | 2.5",
            "7 % 3               | 1",
            "2 ^ 10              | 1024",
            "2 ^ 3 ^ 2           | 512",
            "-2 ^ 2              | -4",
            "(-2) ^ 2            | 4",
            "2 ^ -1              | 0.5",
            "--3                 | 3",
            "+5                  | 5",
            "5!                  | 120",
            "3!!                 | 720",
            "0!                  | 1",
            "2pi                 | 6.283185307179586",
            "2(3 + 4)            | 14",
            "(1 + 2)(3 + 4)      | 21",
            "2 pi                | 6.283185307179586",
            "sqrt(16)            | 4",
            "sin(0)              | 0",
            "cos(0)              | 1",
            "abs(-7.5)           | 7.5",
            "max(1, 9, 4)        | 9",
            "min(3, -2)          | -2",
            "avg(2, 4, 6)        | 4",
            "pow(2, 8)           | 256",
            "hypot(3, 4)         | 5",
            "log(1000)           | 3",
            "ln(e)               | 1",
            "log2(8)             | 3",
            "floor(2.7)          | 2",
            "ceil(2.1)           | 3",
            "round(2.5)          | 3",
            "deg(pi)             | 180",
            "rad(180)            | 3.141592653589793",
            "1e3 + 1             | 1001",
            "2.5E-1              | 0.25",
            "1.5 × 2             | 3",
            "9 ÷ 3               | 3",
            "5 − 7               | -2",
            "mod(10, 4)          | 2",
            "fact(4)             | 24",
            "PI                  | 3.141592653589793",
            "SQRT(81)            | 9",
            "phi                 | 1.618033988749895",
    })
    void evaluatesExpressions(String expression, double expected) {
        assertEquals(expected, calc.evaluate(expression), 1e-9, expression);
    }

    @Test
    @DisplayName("nome de função sem parênteses é tratado como identificador desconhecido")
    void functionNameWithoutParensIsVariable() {
        ExpressionException ex = assertThrows(ExpressionException.class, () -> calc.evaluate("sqrt"));
        assertTrue(ex.getMessage().contains("sqrt"));
        assertEquals(0, ex.position());
    }

    @Test
    @DisplayName("multiplicação implícita também vale dentro de argumentos: max(1 2) == max(2)")
    void implicitMultiplicationInsideArguments() {
        assertEquals(2, calc.evaluate("max(1 2)"), 0.0);
    }

    @Test
    @DisplayName("ans guarda o último resultado")
    void ansKeepsLastResult() {
        assertFalse(calc.hasResult());
        assertEquals(0, calc.evaluate("ans"), 0.0);

        calc.evaluate("6 * 7");
        assertTrue(calc.hasResult());
        assertEquals(42, calc.lastResult(), 0.0);
        assertEquals(84, calc.evaluate("ans * 2"), 0.0);
        assertEquals(84, calc.lastResult(), 0.0);
    }

    @Test
    @DisplayName("erros de sintaxe apontam a posição correta")
    void syntaxErrorsCarryPosition() {
        assertPosition("1 + ", 4);
        assertPosition("(1 + 2", 6);
        assertPosition("1 + * 2", 4);
        assertPosition("2 $ 3", 2);
        assertPosition("", 0);
        assertPosition("   ", 0);
        assertPosition("1 2 )", 4);
        assertPosition("max(1,", 6);
    }

    private void assertPosition(String expression, int expectedPosition) {
        ExpressionException ex = assertThrows(ExpressionException.class, () -> calc.evaluate(expression),
                "deveria falhar: '" + expression + "'");
        assertEquals(expectedPosition, ex.position(), "posição em '" + expression + "': " + ex.getMessage());
    }

    @Test
    @DisplayName("erros semânticos")
    void semanticErrors() {
        assertThrows(ExpressionException.class, () -> calc.evaluate("foo + 1"));
        assertThrows(ExpressionException.class, () -> calc.evaluate("sqrt(1, 2)"));
        assertThrows(ExpressionException.class, () -> calc.evaluate("pow(2)"));
        assertThrows(ExpressionException.class, () -> calc.evaluate("(-1)!"));
        assertThrows(ExpressionException.class, () -> calc.evaluate("2.5!"));
        assertThrows(ExpressionException.class, () -> calc.evaluate("max()"));
    }

    @Test
    @DisplayName("semântica IEEE para divisão por zero")
    void divisionByZeroFollowsIeee() {
        assertEquals(Double.POSITIVE_INFINITY, calc.evaluate("1/0"), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, calc.evaluate("-1/0"), 0.0);
        assertTrue(Double.isNaN(calc.evaluate("0/0")));
        assertEquals(Double.POSITIVE_INFINITY, calc.evaluate("200!"), 0.0);
    }

    @Test
    @DisplayName("AST é exposta e avaliável de forma independente")
    void astIsInspectable() {
        Expr expr = calc.parse("1 + 2 * x");
        assertInstanceOf(Expr.Binary.class, expr);
        Expr.Binary root = (Expr.Binary) expr;
        assertEquals(Expr.Operator.ADD, root.operator());
        assertInstanceOf(Expr.Binary.class, root.right());

        Environment env = new Environment();
        env.setVariable("x", 10);
        assertEquals(21, expr.evaluate(env), 0.0);
        assertThrows(ExpressionException.class, () -> expr.evaluate(new Environment()));
    }

    @Test
    @DisplayName("funções customizadas podem ser registradas")
    void customFunctions() {
        calc.environment().unary("dobro", x -> x * 2);
        calc.environment().register("soma", 0, Integer.MAX_VALUE, args -> {
            double s = 0;
            for (double a : args) {
                s += a;
            }
            return s;
        });
        assertEquals(10, calc.evaluate("dobro(5)"), 0.0);
        assertEquals(6, calc.evaluate("soma(1, 2, 3)"), 0.0);
        assertEquals(0, calc.evaluate("soma()"), 0.0);
    }

    @ParameterizedTest(name = "format({0}) = {1}")
    @CsvSource(delimiter = '|', value = {
            "0                   | 0",
            "-0.0                | 0",
            "42                  | 42",
            "2.5                 | 2.5",
            "0.1                 | 0.1",
            "0.30000000000000004 | 0.3",
            "3.141592653589793   | 3.14159265359",
            "1234567890123       | 1234567890123",
            "1e15                | 1E15",
            "1.5e20              | 1.5E20",
            "1e-7                | 1E-7",
            "-0.000001234        | -0.000001234",
            "NaN                 | NaN",
            "Infinity            | ∞",
            "-Infinity           | -∞",
    })
    void formatsNumbers(double value, String expected) {
        assertEquals(expected, Calculator.format(value));
    }
}
