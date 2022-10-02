package dawn.cs2.ast;


public interface IContinueableNode extends IControllableFlowNode {
    FlowBlock getStart();
    
    boolean canContinue();
}
