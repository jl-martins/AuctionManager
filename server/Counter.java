public class Counter {
	private int value;
	
	public Counter(){
		this.value = 0;
	}

	public Counter(int value){
		this.value = value;
	}
	
	public synchronized int getValue(){
		return value;
	}
	
	public synchronized void setValue(int value){
		this.value = value;
	}

	public synchronized int increment(){
		return ++value;
	}

	public synchronized int getCurrentValueAndIncrement(){
		return value++;
	}
}	
