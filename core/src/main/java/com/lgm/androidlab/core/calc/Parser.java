package com.lgm.androidlab.core.calc;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser por descida recursiva com precedência clássica:
 *
 * <pre>
 * expression     := additive
 * additive       := multiplicative (('+' | '-') multiplicative)*
 * multiplicative := unary (('*' | '/' | '%') unary | implicit-unary)*
 * unary          := ('-' | '+') unary | power
 * power          := postfix ('^' unary)?           (associativo à direita)
 * postfix        := primary ('!')*
 * primary        := NUMBER | IDENT | IDENT '(' args ')' | '(' expression ')'
 * </pre>
 *
 * <p>Multiplicação implícita é aceita quando um operando vem logo após outro, por exemplo
 * {@code 2pi}, {@code 3(4+1)} ou {@code (1+2)(3+4)}.</p>
 */
public final class Parser {

    private final List<Token> tokens;
    private final Environment env;
    private int index;

    private Parser(List<Token> tokens, Environment env) {
        this.tokens = tokens;
        this.env = env;
    }

    public static Expr parse(String source, Environment env) {
        if (source == null || source.isBlank()) {
            throw new ExpressionException("Expressão vazia", 0);
        }
        Parser parser = new Parser(Tokenizer.tokenize(source), env);
        Expr expr = parser.additive();
        Token last = parser.peek();
        if (last.type() != Token.Type.EOF) {
            throw new ExpressionException("Token inesperado '" + last.text() + "'", last.position());
        }
        return expr;
    }

    private Expr additive() {
        Expr left = multiplicative();
        while (true) {
            Token t = peek();
            if (t.type() == Token.Type.PLUS) {
                next();
                left = new Expr.Binary(Expr.Operator.ADD, left, multiplicative());
            } else if (t.type() == Token.Type.MINUS) {
                next();
                left = new Expr.Binary(Expr.Operator.SUBTRACT, left, multiplicative());
            } else {
                return left;
            }
        }
    }

    private Expr multiplicative() {
        Expr left = unary();
        while (true) {
            Token t = peek();
            switch (t.type()) {
                case STAR -> {
                    next();
                    left = new Expr.Binary(Expr.Operator.MULTIPLY, left, unary());
                }
                case SLASH -> {
                    next();
                    left = new Expr.Binary(Expr.Operator.DIVIDE, left, unary());
                }
                case PERCENT -> {
                    next();
                    left = new Expr.Binary(Expr.Operator.MODULO, left, unary());
                }
                case NUMBER, IDENTIFIER, LPAREN ->
                    // multiplicação implícita: "2pi", "2(3)", "(1)(2)"
                    left = new Expr.Binary(Expr.Operator.MULTIPLY, left, unary());
                default -> {
                    return left;
                }
            }
        }
    }

    private Expr unary() {
        Token t = peek();
        if (t.type() == Token.Type.MINUS) {
            next();
            return new Expr.Negate(unary());
        }
        if (t.type() == Token.Type.PLUS) {
            next();
            return unary();
        }
        return power();
    }

    private Expr power() {
        Expr base = postfix();
        if (peek().type() == Token.Type.CARET) {
            next();
            return new Expr.Binary(Expr.Operator.POWER, base, unary());
        }
        return base;
    }

    private Expr postfix() {
        Expr expr = primary();
        while (peek().type() == Token.Type.BANG) {
            Token bang = next();
            expr = new Expr.Factorial(expr, bang.position());
        }
        return expr;
    }

    private Expr primary() {
        Token t = next();
        switch (t.type()) {
            case NUMBER -> {
                try {
                    return new Expr.Number(Double.parseDouble(t.text()));
                } catch (NumberFormatException e) {
                    throw new ExpressionException("Número inválido '" + t.text() + "'", t.position());
                }
            }
            case IDENTIFIER -> {
                if (peek().type() == Token.Type.LPAREN && env.hasFunction(t.text())) {
                    next();
                    List<Expr> args = arguments(t);
                    return new Expr.Call(t.text(), args, t.position());
                }
                return new Expr.Variable(t.text(), t.position());
            }
            case LPAREN -> {
                Expr inner = additive();
                expect(Token.Type.RPAREN, "')'");
                return inner;
            }
            case EOF -> throw new ExpressionException("Expressão incompleta", t.position());
            default -> throw new ExpressionException("Token inesperado '" + t.text() + "'", t.position());
        }
    }

    private List<Expr> arguments(Token functionToken) {
        List<Expr> args = new ArrayList<>();
        if (peek().type() == Token.Type.RPAREN) {
            next();
            return args;
        }
        while (true) {
            args.add(additive());
            Token t = next();
            if (t.type() == Token.Type.RPAREN) {
                return args;
            }
            if (t.type() != Token.Type.COMMA) {
                throw new ExpressionException("Esperado ',' ou ')' nos argumentos de '"
                        + functionToken.text() + "'", t.position());
            }
        }
    }

    private void expect(Token.Type type, String description) {
        Token t = next();
        if (t.type() != type) {
            throw new ExpressionException("Esperado " + description, t.position());
        }
    }

    private Token peek() {
        return tokens.get(index);
    }

    private Token next() {
        Token t = tokens.get(index);
        if (t.type() != Token.Type.EOF) {
            index++;
        }
        return t;
    }
}
