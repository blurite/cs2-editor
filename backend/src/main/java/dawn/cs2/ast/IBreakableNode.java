package dawn.cs2.ast;


public interface IBreakableNode extends IControllableFlowNode {
    FlowBlock getEnd();
    
    boolean canBreak();
}
