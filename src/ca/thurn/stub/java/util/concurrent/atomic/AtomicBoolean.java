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
	
	public boolean getAndSet(boolean newValue) {
	  boolean old = value;
	  value = newValue;
	  return old;
	}
}