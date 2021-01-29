package com.pydgeon.calculator;

import java.util.Stack;

public class Calculator {

    public enum Operator{ADD, SUBTRACT, MULTIPLY, DIVIDE, BLANK}

    public int compute(String sequence){
        Stack<Integer> numberStack = new Stack<Integer>();
        Stack<Operator> operatorStack = new Stack<Operator>();
        for(int i = 0; i < sequence.length(); i++){
            try{
                int number = parseNumber(sequence, i);
                numberStack.push((int)number);

                i += Integer.toString(number).length();
                if(i >= sequence.length()){
                    break;
                }

                Operator op = parseOperator(sequence, i);
                collapseTop(numberStack, operatorStack, op);
                operatorStack.push(op);
            } catch(NumberFormatException ex){
                return Integer.MIN_VALUE;
            }
        }
        collapseTop(numberStack, operatorStack, Operator.BLANK);
        if(numberStack.size() == 1 && operatorStack.size() == 0){
            return numberStack.pop();
        }
        return 0;
    }

    private void collapseTop(Stack<Integer> numberStack, Stack<Operator> operatorStack, Operator futureTop){
        while(numberStack.size() >= 2 && operatorStack.size() >= 1){
            if(priorityOfOperator(futureTop) <= priorityOfOperator(operatorStack.peek())){
                int second = numberStack.pop();
                int first = numberStack.pop();
                Operator op = operatorStack.pop();
                int result = applyOp(first, op, second);
                numberStack.push(result);
            } else{
                break;
            }
        }
    }

    private int applyOp(int left, Operator op, int right){
        switch (op){
            case ADD: return left + right;
            case SUBTRACT: return left - right;
            case MULTIPLY: return left * right;
            case DIVIDE: return left / right;
            default: return right;
        }

    }

    private int priorityOfOperator(Operator op){
        switch (op){
            case ADD: return 1;
            case SUBTRACT: return 1;
            case MULTIPLY: return 2;
            case DIVIDE: return 2;
            case BLANK: return 0;
        }
        return 0;
    }

    private int parseNumber(String sequence, int offset){
        StringBuilder sb = new StringBuilder();
        while(offset < sequence.length() && Character.isDigit(sequence.charAt(offset))){
            sb.append(sequence.charAt(offset));
            offset++;
        }
        return Integer.parseInt(sb.toString());
    }

    private Operator parseOperator(String sequence, int offset){
        if(offset < sequence.length()){
            char op = sequence.charAt(offset);
            switch (op){
                case '+': return Operator.ADD;
                case '-': return Operator.SUBTRACT;
                case '*': return Operator.MULTIPLY;
                case '/': return Operator.DIVIDE;
            }
        }
        return Operator.BLANK;
    }

}
