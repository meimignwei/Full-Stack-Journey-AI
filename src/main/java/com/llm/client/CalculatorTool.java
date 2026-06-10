package com.llm.client;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 安全计算器工具 —— 支持四则运算和基本数学函数
 * 使用双栈算法解析表达式，不依赖 ScriptEngine（安全，无代码注入风险）
 */
public class CalculatorTool implements Tool {

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "执行数学计算。支持加减乘除、括号、幂运算、sqrt、abs。当需要进行数值计算时使用此工具。";
    }

    @Override
    public String getParametersJsonSchema() {
        return "{" +
                "\"type\":\"object\"," +
                "\"properties\":{" +
                    "\"expression\":{" +
                        "\"type\":\"string\"," +
                        "\"description\":\"数学表达式，如 '(1+2)*3'、'sqrt(16)'、'2^10'\"" +
                    "}" +
                "}," +
                "\"required\":[\"expression\"]" +
                "}";
    }

    @Override
    public String execute(String argumentsJson) {
        String expr = SearchTool.extractStringField(argumentsJson, "expression");
        if (expr == null || expr.isBlank()) {
            return "错误：缺少数学表达式";
        }
        try {
            double result = evaluate(expr);
            // 整数用整数格式输出
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return "计算结果：" + expr + " = " + (long) result;
            }
            return "计算结果：" + expr + " = " + result;
        } catch (Exception e) {
            return "计算出错：" + e.getMessage();
        }
    }

    /** 双栈算法 —— 表达式求值 */
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
            } else if (op == ')') {
                break;
            } else {
                break;
            }
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
            } else {
                break;
            }
        }

        while (!ops.isEmpty()) {
            double b = values.pop();
            double a = values.pop();
            values.push(applyOp(ops.pop(), a, b));
        }
        return values.pop();
    }

    private double parseFactor(ParseState s) {
        if (s.pos >= s.expr.length()) {
            throw new IllegalArgumentException("表达式不完整");
        }

        char c = s.expr.charAt(s.pos);

        // 一元负号
        if (c == '-') {
            s.pos++;
            return -parseFactor(s);
        }

        // 括号
        if (c == '(') {
            s.pos++;
            double val = parseExpression(s);
            if (s.pos < s.expr.length() && s.expr.charAt(s.pos) == ')') {
                s.pos++;
            }
            return val;
        }

        // 函数
        if (Character.isLetter(c)) {
            String func = readWord(s);
            if (s.pos < s.expr.length() && s.expr.charAt(s.pos) == '(') {
                s.pos++;
                double arg = parseExpression(s);
                if (s.pos < s.expr.length() && s.expr.charAt(s.pos) == ')') {
                    s.pos++;
                }
                return applyFunc(func, arg);
            }
            // 常量
            if ("pi".equalsIgnoreCase(func)) return Math.PI;
            if ("e".equalsIgnoreCase(func))  return Math.E;
            throw new IllegalArgumentException("未知标识符: " + func);
        }

        // 数字
        return parseNumber(s);
    }

    private String readWord(ParseState s) {
        int start = s.pos;
        while (s.pos < s.expr.length() && Character.isLetter(s.expr.charAt(s.pos))) {
            s.pos++;
        }
        return s.expr.substring(start, s.pos);
    }

    private double parseNumber(ParseState s) {
        int start = s.pos;
        while (s.pos < s.expr.length() &&
                (Character.isDigit(s.expr.charAt(s.pos)) || s.expr.charAt(s.pos) == '.')) {
            s.pos++;
        }
        return Double.parseDouble(s.expr.substring(start, s.pos));
    }

    private double applyOp(char op, double a, double b) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/':
                if (b == 0) throw new ArithmeticException("除以零");
                return a / b;
            case '^': return Math.pow(a, b);
            default: throw new IllegalArgumentException("未知运算符: " + op);
        }
    }

    private double applyFunc(String func, double arg) {
        switch (func.toLowerCase()) {
            case "sqrt":   return Math.sqrt(arg);
            case "abs":    return Math.abs(arg);
            case "sin":    return Math.sin(Math.toRadians(arg));
            case "cos":    return Math.cos(Math.toRadians(arg));
            case "log":    return Math.log10(arg);
            case "ln":     return Math.log(arg);
            case "round":  return Math.round(arg);
            case "ceil":   return Math.ceil(arg);
            case "floor":  return Math.floor(arg);
            default: throw new IllegalArgumentException("未知函数: " + func);
        }
    }

    private static class ParseState {
        final String expr;
        int pos;

        ParseState(String expr) {
            this.expr = expr;
            this.pos = 0;
        }
    }
}