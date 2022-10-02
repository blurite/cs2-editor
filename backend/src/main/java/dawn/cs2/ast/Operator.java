package dawn.cs2.ast;

public enum Operator {
    
    PLUS("+", ExpressionNode.PRIORITY_ADDSUB, MathExpressionNode.class, 2),
    MINUS("-", ExpressionNode.PRIORITY_ADDSUB, MathExpressionNode.class, 2),
    MUL("*", ExpressionNode.PRIORITY_MULDIVREM, MathExpressionNode.class, 2),
    DIV("/", ExpressionNode.PRIORITY_MULDIVREM, MathExpressionNode.class, 2),
    REM("%", ExpressionNode.PRIORITY_MULDIVREM, MathExpressionNode.class, 2),
    //TODO:
    PLUSPREFIX("++", ExpressionNode.PRIORITY_PLUSMINUSPREFIXPOSTFIX, ExpressionNode.class, 1, true, false),
    PLUSPOSTFIX("++", ExpressionNode.PRIORITY_PLUSMINUSPREFIXPOSTFIX, ExpressionNode.class, 1),
    MINUSPREFIX("--", ExpressionNode.PRIORITY_PLUSMINUSPREFIXPOSTFIX, ExpressionNode.class, 1, true, false),
    MINUSPOSTFIX("--", ExpressionNode.PRIORITY_PLUSMINUSPREFIXPOSTFIX, ExpressionNode.class, 1),
    UNARYMINUS("-", ExpressionNode.PRIORITY_UNARYPLUSMINUS, ExpressionNode.class, 1, true, false),
    UNARYPLUS("+", ExpressionNode.PRIORITY_UNARYPLUSMINUS, ExpressionNode.class, 1, true, false),
    UNARYNOT("!", ExpressionNode.PRIORITY_UNARYLOGICALNOT, ExpressionNode.class, 1, true, false),
    EQ("==", ExpressionNode.PRIORITY_EQNE, ConditionalExpressionNode.class, 2),
    NEQ("!=", ExpressionNode.PRIORITY_EQNE, ConditionalExpressionNode.class, 2),
    GT(">", ExpressionNode.PRIORITY_LELTGEGTINSTANCEOF, ConditionalExpressionNode.class, 2),
    GE(">=", ExpressionNode.PRIORITY_LELTGEGTINSTANCEOF, ConditionalExpressionNode.class, 2),
    LT("<", ExpressionNode.PRIORITY_LELTGEGTINSTANCEOF, ConditionalExpressionNode.class, 2),
    LE("<=", ExpressionNode.PRIORITY_LELTGEGTINSTANCEOF, ConditionalExpressionNode.class, 2),
    OR("||", ExpressionNode.PRIORITY_LOGICALOR, ConditionalExpressionNode.class, 2),
    AND("&&", ExpressionNode.PRIORITY_LOGICALAND, ConditionalExpressionNode.class, 2),
    ASSIGN("=", ExpressionNode.PRIORITY_ASSIGNMENT, VariableAssignationNode.class, 2, false, true),
    //TODO: += etc
    
    DUMMY_OP("", 99, ExpressionNode.class, 0);
    
    
    public final String text;
    public final int priority;
    public final Class<? extends ExpressionNode> type;
    public final int operands;
    public boolean prefix;
    public boolean assocRight;
    
    Operator(String text, int prio, Class<? extends ExpressionNode> type, int operands) {
        this(text, prio, type, operands, false, false);
    }
    
    Operator(String text, int prio, Class<? extends ExpressionNode> type, int operands, boolean prefix, boolean assocRight) {
        this.text = text;
        this.priority = prio;
        this.type = type;
        this.operands = operands;
        this.prefix = prefix;
        this.assocRight = assocRight;
    }
    
    @Override
    public String toString() {
        return text;
    }
}
