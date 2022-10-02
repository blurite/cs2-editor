package dawn.cs2.ast;

import dawn.cs2.CodePrinter;

public abstract class AbstractCodeNode {
    
    public AbstractCodeNode() {
    
    }
    
    public abstract void print(CodePrinter printer);
    
    @Override
    public String toString() {
//		return "disabled";
        return CodePrinter.print(this);
    }
    
}
