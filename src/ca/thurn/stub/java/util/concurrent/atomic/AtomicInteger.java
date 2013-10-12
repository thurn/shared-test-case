package java.util.concurrent.atomic;

public class AtomicInteger {
  int value;
  
  public AtomicInteger(int value) {
    this.value = value;
  }
  
  public void set(int value) {
    this.value = value;
  }
  
  public int get() {
    return value;
  }
  
  public int getAndIncrement() {
    return value++;
  }
  
  public int getAndDecrement() {
    return value--;
  }
}