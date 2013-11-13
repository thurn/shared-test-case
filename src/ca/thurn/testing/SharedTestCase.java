package ca.thurn.testing;

import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;
import com.jayway.awaitility.Awaitility;

/**
 * A subclass for sharing asynchronous GWTTestCase tests between client-side and server-side code.
 * 
 * Usage: At the end of your asynchronous test's main body, invoke awaitFinished(). On the server,
 * this blocks the test thread. On the client, this sets up a listener. When your asynchronous
 * callback completes, invoke finished() to unblock the thread (on the server) or mark the test
 * finished (on the client).
 */
public abstract class SharedTestCase extends GWTTestCase {

  final AtomicBoolean finished = new AtomicBoolean(false);
  final AtomicInteger numFinishes = new AtomicInteger(0);
  final AtomicBoolean didSetUpTestCase = new AtomicBoolean(false);

  public static enum TestMode {
    JAVA,
    JAVASCRIPT,
    OBJECTIVE_C
  }

  @Override
  public String getModuleName() {
    switch(getTestMode()) {
	  case JAVA:
	    return null;
	  case JAVASCRIPT:
		return getJavascriptModuleName();
	  default:
		throw new IllegalStateException("Unexpected TestMode");
	}
  }
  
  public abstract String getJavascriptModuleName();
  
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
  
  public final void gwtSetUp() {
    beginAsyncTestBlock();
    final Runnable runFinished = new OneTimeRunnable(new Runnable() {
      @Override
      public void run() {
        finished();
      }});
    sharedSetUpTestCase(runFinished);    
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
    if (getTestMode() != TestMode.JAVASCRIPT) {
      if (onComplete != null) {
        onComplete.run();
      }
    } else { 
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
  }
  
  public void beginAsyncTestBlock() {
    beginAsyncTestBlock(1);
  }

  public synchronized void beginAsyncTestBlock(int numFinishesExpected) {
    numFinishes.set(numFinishesExpected);
    if (!isPureJava()) {
      delayTestFinish(10000);
    }
  }

  public void endAsyncTestBlock() {
    if (isPureJava()) {
      Awaitility.await("Waiting for call to finished()").untilTrue(finished);
      finished.set(false);
    }
  }

  public TestMode getTestMode() {
    if (System.getProperty("test.mode", "client").equals("server")) {
      return TestMode.JAVA;
    } else {
      return TestMode.JAVASCRIPT;
    } // OBJECTIVE_C is a separate .java file.
  }

  /**
   * Indicates that your test, where you previously called awaitFinished(), is done executing.
   */
  public synchronized void finished() {
    numFinishes.getAndDecrement();
    if (numFinishes.get() <= 0) {
      if (isPureJava()) {
        finished.set(true);
      } else {
        finishTest();
      }
    }
  }

  public void schedule(int delayMillis, final Runnable runnable) {
    if (isPureJava()) {
      new java.util.Timer().schedule(new TimerTask() {
        @Override
        public void run() {
          runnable.run();
        }
      }, delayMillis);
    } else {
      (new Timer() {
        @Override
        public void run() {
          runnable.run();
        }
      }).schedule(delayMillis);
    }
  }

  public int randomInteger() {
    if (isPureJava()) {
      return new java.util.Random().nextInt();
    } else {
      return com.google.gwt.user.client.Random.nextInt();
    }
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
