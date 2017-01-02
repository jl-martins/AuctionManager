public class RunFlag {
    private boolean value;
    
    public RunFlag() {
        value = true;
    }
    
    public RunFlag(boolean startingValue) {
        value = startingValue;
    }
    
    public synchronized boolean getValue() {
        return value;
    }
    
    public synchronized void setValue(boolean value) {
        this.value = value;
    }
}
