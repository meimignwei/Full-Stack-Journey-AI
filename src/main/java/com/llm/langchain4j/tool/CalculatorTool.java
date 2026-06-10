package com.llm.langchain4j.tool;

import dev.langchain4j.agent.tool.Tool;

public class CalculatorTool {

    @Tool("执行数学计算，支持加减乘除和 sqrt 开方，例如: 38*47+sqrt(256)")
    public String calculator(String expression) {
        try {
            double result = eval(expression);
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return String.valueOf((long) result);
            }
            return String.valueOf(result);
        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }

    private double eval(String expr) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() { ch = (++pos < expr.length()) ? expr.charAt(pos) : -1; }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) { nextChar(); return true; }
                return false;
            }

            double parse() { nextChar(); double x = parseExpression(); if (pos < expr.length()) throw new RuntimeException("Unexpected: " + (char)ch); return x; }
            double parseExpression() { double x = parseTerm(); for (;;) { if (eat('+')) x += parseTerm(); else if (eat('-')) x -= parseTerm(); else return x; } }
            double parseTerm() { double x = parseFactor(); for (;;) { if (eat('*')) x *= parseFactor(); else if (eat('/')) x /= parseFactor(); else return x; } }
            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();
                double x; int startPos = this.pos;
                if (eat('(')) { x = parseExpression(); eat(')'); }
                else if ((ch >= '0' && ch <= '9') || ch == '.') { while ((ch >= '0' && ch <= '9') || ch == '.') nextChar(); x = Double.parseDouble(expr.substring(startPos, this.pos)); }
                else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                    while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '.') nextChar();
                    String func = expr.substring(startPos, this.pos);
                    if (func.equalsIgnoreCase("sqrt") || func.equalsIgnoreCase("Math.sqrt")) {
                        eat('('); x = Math.sqrt(parseExpression()); eat(')');
                    } else throw new RuntimeException("Unknown: " + func);
                }
                else throw new RuntimeException("Unexpected: " + (char)ch);
                if (eat('^')) x = Math.pow(x, parseFactor());
                return x;
            }
        }.parse();
    }
}