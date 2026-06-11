package com.llm.springai.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

@Component
public class CalculatorTool implements Function<CalculatorTool.Request, String> {

    public record Request(
            String expression
    ) {}

    @Override
    public String apply(Request request) {
        String expr = request.expression;
        if (expr == null || expr.isBlank()) {
            return "错误：缺少数学表达式";
        }
        try {
            double result = evaluate(expr);
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return "计算结果：" + expr + " = " + (long) result;
            }
            return "计算结果：" + expr + " = " + result;
        } catch (Exception e) {
            return "计算出错：" + e.getMessage();
        }
    }

    private double evaluate(String expr) {
        expr = expr.replaceAll("\\s+", "");
        return parseExpression(new ParseState(expr));
    }

    private double parseExpression(ParseState s) {
        Deque<Double> values = new ArrayDeque<>();
        Deque<Character> ops = new ArrayDeque<>();
        values.push(parseTerm(s));
        while (s.pos < s.expr.length()) {
            char op = s.expr.charAt(s.pos);
            if (op == '+' || op == '-') {
                while (!ops.isEmpty()) {
                    double b = values.pop();
                    double a = values.pop();
                    values.push(applyOp(ops.pop(), a, b));
                }
                ops.push(op);
                s.pos++;
                values.push(parseTerm(s));
            } else if (op == ')') break;
            else break;
        }
        while (!ops.isEmpty()) {
            double b = values.pop();
            double a = values.pop();
            values.push(applyOp(ops.pop(), a, b));
        }
        return values.pop();
    }

    private double parseTerm(ParseState s) {
        Deque<Double> values = new ArrayDeque<>();
        Deque<Character> ops = new ArrayDeque<>();
        values.push(parseFactor(s));
        while (s.pos < s.expr.length()) {
            char op = s.expr.charAt(s.pos);
            if (op == '*' || op == '/') {
                while (!ops.isEmpty() && (ops.peek() == '*' || ops.peek() == '/')) {
                    double b = values.pop();
                    double a = values.pop();
                    values.push(applyOp(ops.pop(), a, b));
                }
                ops.push(op);
                s.pos++;
                values.push(parseFactor(s));
            } else if (op == '^') {
                ops.push(op);
                s.pos++;
                values.push(parseFactor(s));
            } else break;
        }
        while (!ops.isEmpty()) {
            double b = values.pop();
            double a = values.pop();
            values.push(applyOp(ops.pop(), a, b));
        }
        return values.pop();
    }

    private double parseFactor(ParseState s) {
        if (s.pos >= s.expr.length()) throw new IllegalArgumentException("表达式不完整");
        char c = s.expr.charAt(s.pos);
        if (c == '-') { s.pos++; return -parseFactor(s); }
        if (c == '(') {
            s.pos++;
            double val = parseExpression(s);
            if (s.pos < s.expr.length() && s.expr.charAt(s.pos) == ')') s.pos++;
            return val;
        }
        if (Character.isLetter(c)) {
            String func = readWord(s);
            if (s.pos < s.expr.length() && s.expr.charAt(s.pos) == '(') {
                s.pos++;
                double arg = parseExpression(s);
                if (s.pos < s.expr.length() && s.expr.charAt(s.pos) == ')') s.pos++;
                return applyFunc(func, arg);
            }
            if ("pi".equalsIgnoreCase(func)) return Math.PI;
            if ("e".equalsIgnoreCase(func)) return Math.E;
            throw new IllegalArgumentException("未知标识符: " + func);
        }
        return parseNumber(s);
    }

    private String readWord(ParseState s) {
        int start = s.pos;
        while (s.pos < s.expr.length() && Character.isLetter(s.expr.charAt(s.pos))) s.pos++;
        return s.expr.substring(start, s.pos);
    }

    private double parseNumber(ParseState s) {
        int start = s.pos;
        while (s.pos < s.expr.length() && (Character.isDigit(s.expr.charAt(s.pos)) || s.expr.charAt(s.pos) == '.')) s.pos++;
        return Double.parseDouble(s.expr.substring(start, s.pos));
    }

    private double applyOp(char op, double a, double b) {
        return switch (op) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> { if (b == 0) throw new ArithmeticException("除以零"); yield a / b; }
            case '^' -> Math.pow(a, b);
            default -> throw new IllegalArgumentException("未知运算符: " + op);
        };
    }

    private double applyFunc(String func, double arg) {
        return switch (func.toLowerCase()) {
            case "sqrt" -> Math.sqrt(arg);
            case "abs" -> Math.abs(arg);
            case "sin" -> Math.sin(Math.toRadians(arg));
            case "cos" -> Math.cos(Math.toRadians(arg));
            case "log" -> Math.log10(arg);
            case "ln" -> Math.log(arg);
            case "round" -> Math.round(arg);
            case "ceil" -> Math.ceil(arg);
            case "floor" -> Math.floor(arg);
            default -> throw new IllegalArgumentException("未知函数: " + func);
        };
    }

    private static class ParseState {
        final String expr;
        int pos;
        ParseState(String expr) { this.expr = expr; this.pos = 0; }
    }
}
