package java.util.concurrent.atomic;

public class AtomicBoolean {
    boolean value;
  
	public AtomicBoolean(boolean value) {
	  this.value = value;
	}
	
	public void set(boolean value) {
	  this.value = value;
	}
	
	public boolean get() {
	  return value;
	}
}