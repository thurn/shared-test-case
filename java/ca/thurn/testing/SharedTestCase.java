package ca.thurn.testing;

import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.jayway.awaitility.Awaitility;

public abstract class SharedTestCase extends TestCase {

  final AtomicBoolean finished = new AtomicBoolean(false);
  final AtomicInteger numFinishes = new AtomicInteger(0);
  final AtomicBoolean didSetUpTestCase = new AtomicBoolean(false);

  public static enum TestMode {
    JAVA,
    JAVASCRIPT,
    OBJECTIVE_C
  }
  
  static class OneTimeRunnable implements Runnable {
    private final AtomicBoolean ran = new AtomicBoolean(false);
    private final Runnable runnable;
    
    OneTimeRunnable(Runnable runnable) {
      this.runnable = runnable;
    }
    
    @Override
    public void run() {
      if (ran.getAndSet(true) == false) {
        runnable.run();
      }
    }
  }
  
  public static class BooleanReference {
    private AtomicBoolean value;
    
    public BooleanReference(boolean value) {
      this.value = new AtomicBoolean(value);
    }
    
    public void set(boolean newValue) {
      value.set(newValue);
    }
    
    public boolean get() {
      return value.get();
    }
    
    public boolean getAndSet(boolean newValue) {
      return value.getAndSet(newValue);
    }
  }
  
  public static class IntegerReference {
    private AtomicInteger value;
    
    public IntegerReference(int value) {
      this.value = new AtomicInteger(value);
    }
    
    public void set(int newValue) {
      value.set(newValue);
    }
    
    public int get() {
      return value.get();
    }
    
    public int getAndSet(int newValue) {
      return value.getAndSet(newValue);
    }
    
    public int getAndIncrement() {
      return value.getAndIncrement();
    }
  }
  
  public String getJavascriptModuleName() {
    throw new RuntimeException("No Javascript module name supplied");
  }
  
  @Override
  public final void setUp() {
    beginAsyncTestBlock();
    final Runnable runFinished = new OneTimeRunnable(new Runnable() {
      @Override
      public void run() {
        finished();
      }});
    if (didSetUpTestCase.getAndSet(true) == false) {
      Runnable runSetUp = new OneTimeRunnable(new Runnable() {
        @Override
        public void run() {
          sharedSetUp(runFinished);
        }
      });
      sharedSetUpTestCase(runSetUp);
    } else {
      sharedSetUp(runFinished);
    }
    endAsyncTestBlock();
  }
  
  @Override
  public final void tearDown() {
    sharedTearDown();
  }
  
  public void sharedSetUpTestCase(Runnable done) {
    done.run();
  }
  
  public void sharedSetUp(Runnable done) {
    done.run();
  }
  
  public void sharedTearDown() {
  }
  
  public void injectScript(String url, final Runnable onComplete) {
    if (onComplete != null) {
      onComplete.run();
    }
  }
  
  public void beginAsyncTestBlock() {
    beginAsyncTestBlock(1);
  }

  public synchronized void beginAsyncTestBlock(int numFinishesExpected) {
    numFinishes.set(numFinishesExpected);
  }

  public void endAsyncTestBlock() {
    Awaitility.await("Waiting for call to finished()").untilTrue(finished);
    finished.set(false);
  }

  public TestMode getTestMode() {
    return TestMode.JAVA;
  }

  /**
   * Indicates that your test, where you previously called beginAsyncTestBlock(), is done
   * executing.
   */
  public synchronized void finished() {
    numFinishes.getAndDecrement();
    if (numFinishes.get() <= 0) {
      finished.set(true);
    }
  }

  public void schedule(int delayMillis, final Runnable runnable) {
    new java.util.Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        runnable.run();
      }
    }, delayMillis);
  }

  public int randomInteger() {
    return new java.util.Random().nextInt();
  }

  public void assertDeepEquals(Object o1, Object o2) {
    assertDeepEquals("(no message)", o1, o2);
  }
  
  public void assertDeepEquals(String msg, Object o1, Object o2) {
    if (o1 instanceof Iterable && o2 instanceof Iterable) {
      @SuppressWarnings("unchecked")
      Iterator<Object> ite1 = ((Iterable<Object>) o1).iterator();
      @SuppressWarnings("unchecked")
      Iterator<Object> ite2 = ((Iterable<Object>) o2).iterator();
      while (ite1.hasNext() && ite2.hasNext()) {
        assertDeepEquals(msg, ite1.next(), ite2.next());
      }
      assertFalse("Iterable sizes differ", ite1.hasNext() || ite2.hasNext());
    } else if (o1 instanceof Map && o2 instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<Object, Object> map1 = (Map<Object, Object>) o1;
      @SuppressWarnings("unchecked")
      Map<Object, Object> map2 = (Map<Object, Object>) o2;
      assertEquals("Map sizes differ", map1.size(), map2.size());
      for (Map.Entry<Object, Object> entry : map1.entrySet()) {
        assertTrue(map2.containsKey(entry.getKey()));
        assertDeepEquals(msg, entry.getValue(), map2.get(entry.getKey()));
      }
    } else {
      assertEquals(msg, o1, o2);
    }
  }
  
  
  
  // NOTE(dthurn): These static overrides are needed because Awaitility
  // currently doesn't propagate Errors to the main thread, just Exceptions.
  // I've emailed them about the possibility of supporting all Throwables in
  // a future release.
  
  public static void assertTrue(boolean condition) {
    try {
      Assert.assertTrue(condition);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }
  
  public static void assertFalse(boolean condition) {
    try {
      Assert.assertFalse(condition);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }
  
  public static void assertTrue(String msg, boolean condition) {
    try {
      Assert.assertTrue(msg, condition);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }
  
  public static void assertFalse(String msg, boolean condition) {
    try {
      Assert.assertFalse(msg, condition);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }
  
  public static void assertEquals(String s1, String s2) {
    try {
      Assert.assertEquals(s1, s2);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }

  public static void assertEquals(int i1, int i2) {
    try {
      Assert.assertEquals(i1, i2);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }
  
  public static void assertEquals(long l1, long l2) {
    try {
      Assert.assertEquals(l1, l2);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }
  
  public static void assertEquals(String msg, String s1, String s2) {
    try {
      Assert.assertEquals(msg, s1, s2);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }

  public static void assertEquals(String msg, int i1, int i2) {
    try {
      Assert.assertEquals(msg, i1, i2);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }
  
  public static void assertEquals(String msg, long l1, long l2) {
    try {
      Assert.assertEquals(msg, l1, l2);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }
  
  public static void assertEquals(Object o1, Object o2) {
    try {
      Assert.assertEquals(o1, o2);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }
  
  public static void assertEquals(String msg, Object o1, Object o2) {
    try {
      Assert.assertEquals(msg, o1, o2);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }
  
  public static void assertNotNull(Object o) {
    try {
      Assert.assertNotNull(o);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }
  
  public static void fail(String msg) {
    try {
      Assert.fail(msg);
    } catch (Throwable afe) {
      throw new RuntimeException(afe);
    }
  }
  
}
