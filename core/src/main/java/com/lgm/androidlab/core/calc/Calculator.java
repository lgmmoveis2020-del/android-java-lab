package com.lgm.androidlab.core.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Fachada da calculadora: avalia textos, lembra o último resultado em {@code ans}
 * e formata números para exibição.
 */
public final class Calculator {

    private static final MathContext DISPLAY_PRECISION = new MathContext(12, RoundingMode.HALF_EVEN);

    private final Environment env = new Environment();
    private double lastResult;
    private boolean hasResult;

    public Calculator() {
        env.setVariable("ans", 0);
    }

    public Environment environment() {
        return env;
    }

    public Expr parse(String source) {
        return Parser.parse(source, env);
    }

    /**
     * Avalia a expressão e atualiza {@code ans}.
     *
     * @throws ExpressionException se houver erro de sintaxe ou de avaliação
     */
    public double evaluate(String source) {
        double value = parse(source).evaluate(env);
        lastResult = value;
        hasResult = true;
        env.setVariable("ans", value);
        return value;
    }

    public double lastResult() {
        return lastResult;
    }

    public boolean hasResult() {
        return hasResult;
    }

    /**
     * Formata para no máximo 12 algarismos significativos, sem zeros à direita
     * nem ".0" em inteiros. Infinitos e NaN viram símbolos legíveis.
     */
    public static String format(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "∞" : "-∞";
        }
        if (value == 0) {
            return "0";
        }
        double abs = Math.abs(value);
        if (value == Math.rint(value) && abs < 1e15) {
            return Long.toString((long) value);
        }
        BigDecimal bd = new BigDecimal(value).round(DISPLAY_PRECISION).stripTrailingZeros();
        if (abs >= 1e15 || abs < 1e-6) {
            String s = String.format(java.util.Locale.ROOT, "%.6E", bd);
            // "1.500000E+20" -> "1.5E20"
            String[] parts = s.split("E");
            String mantissa = new BigDecimal(parts[0]).stripTrailingZeros().toPlainString();
            int exponent = Integer.parseInt(parts[1]);
            return mantissa + "E" + exponent;
        }
        return bd.toPlainString();
    }
}
