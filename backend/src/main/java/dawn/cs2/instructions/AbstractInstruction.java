package dawn.cs2.instructions;

public class AbstractInstruction {
    
    private int opcode;
    private int address;
    
    public AbstractInstruction(int opcode) {
        this.opcode = opcode;
        this.address = -1;
    }
    
    public int getOpcode() {
        return opcode;
    }
    
    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }
    
    public int getAddress() {
        return address;
    }
    
    public void setAddress(int address) {
        this.address = address;
    }
    
}
