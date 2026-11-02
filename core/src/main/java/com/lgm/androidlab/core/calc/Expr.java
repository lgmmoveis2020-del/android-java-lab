package com.lgm.androidlab.core.calc;

import java.util.List;

/**
 * Árvore sintática de uma expressão. Cada nó sabe se avaliar contra um {@link Environment}.
 */
public sealed interface Expr {

    double evaluate(Environment env);

    record Number(double value) implements Expr {
        @Override
        public double evaluate(Environment env) {
            return value;
        }
    }

    record Variable(String name, int position) implements Expr {
        @Override
        public double evaluate(Environment env) {
            return env.variable(name).orElseThrow(
                    () -> new ExpressionException("Identificador desconhecido '" + name + "'", position));
        }
    }

    record Negate(Expr operand) implements Expr {
        @Override
        public double evaluate(Environment env) {
            return -operand.evaluate(env);
        }
    }

    record Factorial(Expr operand, int position) implements Expr {
        @Override
        public double evaluate(Environment env) {
            double v = operand.evaluate(env);
            if (v < 0 || v != Math.floor(v)) {
                throw new ExpressionException("Fatorial exige inteiro não negativo", position);
            }
            return Environment.factorial(v);
        }
    }

    record Binary(Operator operator, Expr left, Expr right) implements Expr {
        @Override
        public double evaluate(Environment env) {
            double a = left.evaluate(env);
            double b = right.evaluate(env);
            return switch (operator) {
                case ADD -> a + b;
                case SUBTRACT -> a - b;
                case MULTIPLY -> a * b;
                case DIVIDE -> a / b;
                case MODULO -> a % b;
                case POWER -> Math.pow(a, b);
            };
        }
    }

    record Call(String name, List<Expr> args, int position) implements Expr {
        public Call {
            args = List.copyOf(args);
        }

        @Override
        public double evaluate(Environment env) {
            double[] values = new double[args.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = args.get(i).evaluate(env);
            }
            return env.call(name, values, position);
        }
    }

    enum Operator {
        ADD("+"), SUBTRACT("-"), MULTIPLY("*"), DIVIDE("/"), MODULO("%"), POWER("^");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String symbol() {
            return symbol;
        }
    }
}
