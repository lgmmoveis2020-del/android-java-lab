package com.lgm.androidlab.core.calc;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * Constantes, variáveis e funções disponíveis durante a avaliação.
 *
 * <p>Nomes são comparados sem diferenciar maiúsculas de minúsculas.</p>
 */
public final class Environment {

    /** Função matemática com aridade variável. */
    @FunctionalInterface
    public interface Function {
        double apply(double[] args);
    }

    private record FunctionEntry(int minArgs, int maxArgs, Function function) {
    }

    private final Map<String, Double> variables = new HashMap<>();
    private final Map<String, FunctionEntry> functions = new HashMap<>();

    public Environment() {
        // Constantes
        setVariable("pi", Math.PI);
        setVariable("e", Math.E);
        setVariable("tau", Math.PI * 2);
        setVariable("phi", (1 + Math.sqrt(5)) / 2);
        setVariable("inf", Double.POSITIVE_INFINITY);

        // Funções de um argumento
        unary("sin", Math::sin);
        unary("cos", Math::cos);
        unary("tan", Math::tan);
        unary("asin", Math::asin);
        unary("acos", Math::acos);
        unary("atan", Math::atan);
        unary("sinh", Math::sinh);
        unary("cosh", Math::cosh);
        unary("tanh", Math::tanh);
        unary("sqrt", Math::sqrt);
        unary("cbrt", Math::cbrt);
        unary("abs", Math::abs);
        unary("ln", Math::log);
        unary("log", Math::log10);
        unary("log2", x -> Math.log(x) / Math.log(2));
        unary("exp", Math::exp);
        unary("floor", Math::floor);
        unary("ceil", Math::ceil);
        unary("round", x -> (double) Math.round(x));
        unary("sign", Math::signum);
        unary("deg", Math::toDegrees);
        unary("rad", Math::toRadians);
        unary("fact", Environment::factorial);

        // Funções de dois argumentos
        binary("pow", Math::pow);
        binary("hypot", Math::hypot);
        binary("atan2", Math::atan2);
        binary("mod", (a, b) -> a % b);

        // Funções variádicas
        register("min", 1, Integer.MAX_VALUE, args -> {
            double m = args[0];
            for (double a : args) {
                m = Math.min(m, a);
            }
            return m;
        });
        register("max", 1, Integer.MAX_VALUE, args -> {
            double m = args[0];
            for (double a : args) {
                m = Math.max(m, a);
            }
            return m;
        });
        register("avg", 1, Integer.MAX_VALUE, args -> {
            double sum = 0;
            for (double a : args) {
                sum += a;
            }
            return sum / args.length;
        });
    }

    public void setVariable(String name, double value) {
        variables.put(normalize(name), value);
    }

    public Optional<Double> variable(String name) {
        return Optional.ofNullable(variables.get(normalize(name)));
    }

    public boolean hasFunction(String name) {
        return functions.containsKey(normalize(name));
    }

    public void unary(String name, DoubleUnaryOperator op) {
        register(name, 1, 1, args -> op.applyAsDouble(args[0]));
    }

    public void binary(String name, DoubleBinaryOperator op) {
        register(name, 2, 2, args -> op.applyAsDouble(args[0], args[1]));
    }

    public void register(String name, int minArgs, int maxArgs, Function function) {
        functions.put(normalize(name), new FunctionEntry(minArgs, maxArgs, function));
    }

    double call(String name, double[] args, int position) {
        FunctionEntry entry = functions.get(normalize(name));
        if (entry == null) {
            throw new ExpressionException("Função desconhecida '" + name + "'", position);
        }
        if (args.length < entry.minArgs() || args.length > entry.maxArgs()) {
            String expected = entry.minArgs() == entry.maxArgs()
                    ? String.valueOf(entry.minArgs())
                    : "pelo menos " + entry.minArgs();
            throw new ExpressionException("'" + name + "' espera " + expected
                    + " argumento(s), recebeu " + args.length, position);
        }
        return entry.function().apply(args);
    }

    static double factorial(double n) {
        if (n < 0 || n != Math.floor(n)) {
            return Double.NaN;
        }
        if (n > 170) {
            return Double.POSITIVE_INFINITY;
        }
        double result = 1;
        for (int i = 2; i <= (int) n; i++) {
            result *= i;
        }
        return result;
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
