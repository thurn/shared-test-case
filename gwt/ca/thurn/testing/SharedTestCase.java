package ca.thurn.testing;

import java.util.Iterator;
import java.util.Map;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

/**
 * A subclass for sharing asynchronous GWTTestCase tests between client-side and server-side code.
 * 
 * Usage: At the end of your asynchronous test's main body, invoke awaitFinished(). On the server,
 * this blocks the test thread. On the client, this sets up a listener. When your asynchronous
 * callback completes, invoke finished() to unblock the thread (on the server) or mark the test
 * finished (on the client).
 */
public abstract class SharedTestCase extends GWTTestCase {

  boolean finished = false;
  int numFinishes = 0;
  boolean didSetUpTestCase = false;

  public static enum TestMode {
    JAVA,
    JAVASCRIPT,
    OBJECTIVE_C
  }

  @Override
  public String getModuleName() {
    return getJavascriptModuleName();
  }

  static class OneTimeRunnable implements Runnable {
    private boolean ran = false;
    private final Runnable runnable;
    
    OneTimeRunnable(Runnable runnable) {
      this.runnable = runnable;
    }
    
    @Override
    public void run() {
      if (ran == false) {
        ran = true;
        runnable.run();
      }
    }
  }
  
  public static class BooleanReference {
    private boolean value;
    
    public BooleanReference(boolean value) {
      this.value = value;
    }
    
    public void set(boolean newValue) {
      value = newValue;
    }
    
    public boolean get() {
      return value;
    }
    
    public boolean getAndSet(boolean newValue) {
      boolean tmp = value;
      set(newValue);
      return tmp;
    }
  }
  
  public static class IntegerReference {
    private int value;
    
    public IntegerReference(int value) {
      this.value = value;
    }
    
    public void set(int newValue) {
      value = newValue;
    }
    
    public int get() {
      return value;
    }
    
    public int getAndSet(int newValue) {
      int tmp = value;
      set(newValue);
      return tmp;
    }
    
    public int getAndIncrement() {
      return value++;
    }
  }

  public String getJavascriptModuleName() {
    throw new RuntimeException("No Javascript module name supplied");
  }
  
  public final void gwtSetUp() {
    beginAsyncTestBlock();
    final Runnable runFinished = new OneTimeRunnable(new Runnable() {
      @Override
      public void run() {
        finished();
      }});
    sharedSetUpTestCase(runFinished);    
    if (didSetUpTestCase == false) {
      didSetUpTestCase = true;
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
  
  public final void gwtTearDown() {
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
    ScriptInjector.fromUrl(url)
    .setCallback(new Callback<Void, Exception>() {
      @Override
      public void onFailure(Exception reason) {
        throw new RuntimeException(reason);
      }

      @Override
      public void onSuccess(Void result) {
        if (onComplete != null) {
          onComplete.run();
        }
      }
    }).inject();
  }
  
  public void beginAsyncTestBlock() {
    beginAsyncTestBlock(1);
  }

  public synchronized void beginAsyncTestBlock(int numFinishesExpected) {
    numFinishes = numFinishesExpected;
    delayTestFinish(10000);
  }

  public void endAsyncTestBlock() {
  }

  public TestMode getTestMode() {
    return TestMode.JAVASCRIPT;
  }

  /**
   * Indicates that your test, where you previously called awaitFinished(), is done executing.
   */
  public synchronized void finished() {
    numFinishes--;
    if (numFinishes <= 0) {
      finishTest();
    }
  }

  public void schedule(int delayMillis, final Runnable runnable) {
    (new Timer() {
      @Override
      public void run() {
        runnable.run();
      }
    }).schedule(delayMillis);
  }

  public int randomInteger() {
    return com.google.gwt.user.client.Random.nextInt();
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
  
}
