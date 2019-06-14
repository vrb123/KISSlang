package com.kisslang.source.parser;

import com.kisslang.source.parser.ast.*;

import java.util.List;

public final class Parser {

    private static final Token EOF = new Token(TokenType.EOF, "");

    private final List<Token> tokens;
    private final int size;

    private int pos;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        size = tokens.size();
    }

    public Statement parse() {

        final BlockStatement result = new BlockStatement();

        while (!match(TokenType.EOF)) {
            result.addStatement(statement());
        }

        return result;
    }

    private BlockStatement block(){

        final BlockStatement blockOfStatements=new BlockStatement();

        consume(TokenType.LPAREN_FIGURE);

        while (!match(TokenType.RPAREN_FIGURE)){
            if(match(TokenType.EOF)){
                throw new RuntimeException("Expected } , but found end of file");
            }
            blockOfStatements.addStatement(statement());
        }

        return blockOfStatements;
    }

    private Statement blockOrSingle(){

        if(get(0).getType()==TokenType.LPAREN_FIGURE){
            return block();
        }
        return statement();
    }

    private Statement statement(){

        if (match(TokenType.PRINT)){
            return new PrintStatement(expression());
        }
        if(match(TokenType.IF)){
            return IfElse();
        }
        if(match(TokenType.WHILE)){
            return While();
        }
        if(match(TokenType.FOR)){
            return For();
        }

        return assignmentStatement();
    }



    private Statement assignmentStatement() {
        final Token current=get(0);
        if (current.getType()==TokenType.WORD && get(1).getType()==TokenType.ASSIGN){
            consume(TokenType.WORD);
            final String varName=current.getText();
            consume(TokenType.ASSIGN);
            return new AssignmentStatement(varName,expression());
        }
        throw new RuntimeException("Unknown operator!");
    }

    private Statement IfElse() {
        final Expression condition=expression();
        final Statement ifStatement=blockOrSingle();
        Statement elseStatement=null;
        if(match(TokenType.ELSE)) {
            elseStatement = blockOrSingle();
        }
        return new IfConditionalStatement(condition,ifStatement,elseStatement);
    }

    private Statement While() {
        final Expression condition=expression();
        final Statement statementIfTrue=blockOrSingle();
        return new WhileLoopStatement(condition,statementIfTrue);
    }

    private Statement For(){

        consume(TokenType.LPAREN);
        final Statement init=assignmentStatement();
        consume(TokenType.DELIMITER_FOR);
        final Expression term=expression();
        consume(TokenType.DELIMITER_FOR);
        final Statement incr=assignmentStatement();
        consume(TokenType.RPAREN);
        final Statement statements=blockOrSingle();

        return new ForLoopStatement(init,term,incr,statements);

    }

    private Expression expression() {
        return additive();
    }

    private Expression additive() {
        Expression result = conditional();

        while (true) {
            if (match(TokenType.PLUS)) {
                result = new BinaryExpression('+', result, multiplicative());
                continue;
            }
            if (match(TokenType.MINUS)) {
                result = new BinaryExpression('-', result, multiplicative());
                continue;
            }
            break;
        }

        return result;
    }

    private Expression conditional(){
        Expression result = multiplicative();

        while (true) {
            if (match(TokenType.EQUAL)) {
                    result = new ConditionalExpression("==", result, multiplicative());
                    continue;
            }
            if (match(TokenType.LOWER_THAN)) {
                    result = new ConditionalExpression("<", result, multiplicative());
                    continue;
            }
            if (match(TokenType.GREATER_THAN)) {
                    result = new ConditionalExpression(">", result, multiplicative());
                    continue;
            }
            if (match(TokenType.GREATER_OR_EQUAL_THAN)) {
                result = new ConditionalExpression(">=", result, multiplicative());
                continue;
            }
            if (match(TokenType.LOWER_OR_EQUAL_THAN)) {
                result = new ConditionalExpression("<=", result, multiplicative());
                continue;
            }
            break;
        }

        return result;

    }

    private Expression multiplicative() {
        Expression result = logicalOr();

        while (true) {
            // 2 * 6 / 3
            if (match(TokenType.STAR)) {
                result = new BinaryExpression('*', result, logicalOr());
                continue;
            }
            if (match(TokenType.POW)) {
                result = new BinaryExpression('^', result, logicalOr());
                continue;
            }
            if (match(TokenType.SLASH)) {
                result = new BinaryExpression('/', result, logicalOr());
                continue;
            }
            break;
        }

        return result;
    }


    private Expression logicalOr(){

        Expression result = logicalOAnd();

        while (true) {

            if (match(TokenType.OR2)) {
                result = new LogicalBinaryExpression("||", result, logicalOAnd());
                continue;
            }
            if(match(TokenType.OR)){
                result=new LogicalBinaryExpression("|",result,logicalOAnd());
            }
            break;
        }

        return result;

    }

    private Expression logicalOAnd(){

        Expression result = unary();

        while (true) {

            if (match(TokenType.AND2)) {
                result = new LogicalBinaryExpression("&&", result, unary());
                continue;
            }
            if(match(TokenType.AND)){
                result=new LogicalBinaryExpression("&",result,logicalOAnd());
            }
            break;
        }

        return result;

    }

    private Expression unary() {
        if (match(TokenType.MINUS)) {
            return new UnaryExpression('-', primary());
        }
        if (match(TokenType.PLUS)) {
            return primary();
        }
        if (match(TokenType.NOT)) {
            return new LogicalUnaryExpression('!', primary());
        }
        return primary();
    }

    private Expression primary() {
        final Token current = get(0);
        if (match(TokenType.NUMBER)) {
            return new NumberExpression(Double.parseDouble(current.getText()));
        }
        if (match(TokenType.HEX_NUMBER)) {
            return new NumberExpression(Long.parseLong(current.getText(), 16));
        }
        if (match(TokenType.WORD)){
            return new ConstantExpression(current.getText());
        }
        if(match(TokenType.STRING_TEXT)){
            return new StringExpression(current.getText());
        }
        if (match(TokenType.LPAREN)) {
            Expression result = expression();
            match(TokenType.RPAREN);
            return result;
        }
        throw new RuntimeException("Unknown expression");
    }

    private Token consume(TokenType type){
        final Token current=get(0);
        if (type != current.getType()) throw new RuntimeException("Token current doesnt match");
        pos++;
        return current;
    }

    private Token consume(TokenType type,String message){
        final Token current=get(0);
        if (type != current.getType()) throw new RuntimeException(message);
        pos++;
        return current;
    }

    private boolean match(TokenType type) {
        final Token current = get(0);
        if (type != current.getType()) return false;
        pos++;
        return true;
    }

    private void next(){
        pos++;
    }

    private Token get(int relativePosition) {
        final int position = pos + relativePosition;
        if (position >= size) return EOF;
        return tokens.get(position);
    }
}