# asyncframework

这是`ASM+`动态字节码技术实现的一个异步框架，只需要在你的接口上添加一个`@AsyncFunction`注解即可让这个方法异步执行，不依赖任何第三方框架！

其实这个东西我们并不陌生，`asyncframework`的`@AsyncFunction`注解与`spring`框架的`@Async`异步注解实现的功能一样。

A：既然`spring`都已经提供这样的功能，你为什么还要实现一个这样的框架呢？
Q：因为我之前封装组件的时候有需要用到，但又不想为了使用这个功能就把`spring`依赖到项目中，会比较臃肿。其次，也是因为喜欢折腾，想要把自己的想法实现。

`asyncframework`可以取代`spring`的`@Async`使用，只要封装一个`starter`包，依靠`spring`提供的`BeanPostProcess`实现无缝整合。但`spring`都已经提供了，我就不想去造轮子了，`asyncframework`我推荐是在非`spring`项目中使用。

### 版本更新

`2019`年我在微信公众号发表过一篇文章，当时介绍了如果在接口的某个方法上添加一个注解就能让这个方法异步执行的思路，但当时并没有实现带返回值的方法也能异步执行的功能，而`2020`年的今天，我已经把这个功能实现了，也是绞尽脑汁才做出来的。

另外，如果你也对字节码感兴趣，我非常推荐你阅读这个框架的源码，浓缩的都是精华，十几个类包含了设计模式的使用、字节码、以及框架的设计思想，也能较好的理解`spring`的`@Async`是怎么实现的。

在实现支持带返回值的方法异步执行这个功能时，遇到了两个大难题：\
难点一：带返回值的方法如何去实现异步？\
难点二：如何编写字节码实现泛型接口的代理类？

### 如何使用asyncframework ？

第一步：项目中添加依赖

`maven：`
```xml
<dependency>
  <groupId>com.github.wujiuye</groupId>
  <artifactId>asyncframework</artifactId>
  <version>1.2.0-RELEASE</version>
</dependency>
```

`gradle:`
```groovy
implementation 'com.github.wujiuye:asyncframework:1.2.0-RELEASE'
```

第二步：定义接口以及编写接口的实现类

A：为什么需要定义接口？
Q：因为我之前遇到的需求是使用接口，其实想不用写接口更加简单，后面想玩的时候我再加上去。

定义接口：
```java
/**
 * @author wujiuye
 * @version 1.0 on 2019/11/24 
 */
public interface AsyncMessageSubscribe {

    /**
     * 异步无返回值
     *
     * @param queue
     */
    @AsyncFunction
    void pullMessage(String queue);

    /**
     * 异步带返回值
     *
     * @param s1
     * @param s2
     * @return
     */
    @AsyncFunction
    AsyncResult<String> doAction(String s1, String s2);

}
```

编写实现类：
```java
 public class Test{

     /**
     * 接口的实现
     */
    private AsyncMessageSubscribe impl = new AsyncMessageSubscribe() {
        @Override
        public void pullMessage(String queue) {
            System.out.println(queue + "， current thread name:" + Thread.currentThread().getName());
        }

        @Override
        public AsyncResult<String> doAction(String s1, String s2) {
            System.out.println("s1==>" + s1 + ", s2==>" + s2);
            return new AsyncResult<>("hello wujiuye! current thread name:" + Thread.currentThread().getName());
        }
    };

}
```

第三步：配置全局线程池
```java
public class Test{
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
}
```

第四步：使用`AsyncProxyFactory`创建代理对象

调用`AsyncProxyFactory`的`getInterfaceImplSupporAsync`方法创建一个代理类，需要指定异步执行使用哪个线程池，以及接口的实现类。

```java
 public class AsmProxyTest {
 
     @Test
     public void testAutoProxyAsync() throws Exception {
         AsyncMessageSubscribe impl = (String queue) -> System.out.println(queue);
         ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
         AsyncMessageSubscribe proxy = AsmProxyFactory.getInterfaceImplSupporAsync(AsyncMessageSubscribe.class, impl, executorService);
        // 异步不带返回值 
        proxy.pullMessage("wujiuye");
        // 异步带返回值 
        AsyncResult<String> asyncResult = proxy.doAction("sssss", "ddd");
        System.out.println(asyncResult.get());
         System.in.read();
     }
 
 }

```

A：还要创建代理类去调用，我直接`new`一个`Runnable`放线程池执行不是更方便？
Q：确实如此，但如果通过包扫描自动创建代理对象，那就不一样了。`spring`就是通过`BeanPostProcess`实现的。

### 异步带返回值的实现原理

在`spring`项目中，如果想在带返回值的方法上添加`@Async`注解，就需要方法返回值类型为`AsyncResult`，我也去看了一下`spring`的源码，发现`AsyncResult`是一个`Future`，但仅仅只是依靠`Future`还是实现不了的。因为`AsyncResult`是我们在方法执行完成的时候才返回的，方法没执行之前我怎么取到这个`AsyncResult`？并且调用这个`AsyncResult`的`get`方法获取到的结果就是最终的方法返回的结果？

我想到的方法是代理，没错，就是代理代理又代理。我也实现了一个`AsyncResult`，也是一个`Future`，只不过这个`Future`的`get`方法并不是阻塞的，因为不需要阻塞。

```java
/**
 * 异步方法的返回参数
 *
 * @author wujiuye 2020/03/27
 */
public class AsyncResult<T> implements Future<T> {

    private T result;

    public AsyncResult(T result) {
        this.result = result;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return get();
    }

    /**
     * 由字节码调用
     *
     * @param future 提交到线程池执行返回的future
     * @param <T>
     * @return
     */
    public static <T> AsyncResult<T> newAsyncResultProxy(final Future<AsyncResult<T>> future) {
        return new AsyncResult<T>(null) {
            @Override
            public T get() throws InterruptedException, ExecutionException {
                AsyncResult<T> asyncResult = future.get();
                return asyncResult.get();
            }
        };
    }

}
```

`newAsyncResultProxy`方法才是整个异步实现的最关键一步，该方法是给字节码生成的代理对象调用的。

我们知道，`ExecutorService`的`submit`方法支持提交一个`Callable`带返回值的任务，并且`submit`方法返回一个`Future`，调用这个`Future`的`get`方法当前线程会阻塞，直到任务执行结束。那么我们可以把`AsyncResult`的`get`方法代理出来，当调用`AsyncResult`的`get`方法时，实际上是去调用`ExecutorService`的`submit`方法返回的那个`Future`的`get`方法。对使用者屏蔽了这个阻塞获取结果的实现过程。

以前面的使用例子为例，下面的测试代码便是动态字节码要实现的，这便是框架得以支持带返回值的方法异步执行的原理。

```java
public class Test2{

    /**
     * 接口的实现
     */
    private AsyncMessageSubscribe impl = new AsyncMessageSubscribe() {
        @Override
        public void pullMessage(String queue) {
            System.out.println(queue + "， current thread name:" + Thread.currentThread().getName());
        }

        @Override
        public AsyncResult<String> doAction(String s1, String s2) {
            System.out.println("s1==>" + s1 + ", s2==>" + s2);
            return new AsyncResult<>("hello wujiuye! current thread name:" + Thread.currentThread().getName());
        }
    };

    // 提交到线程池执行的Callable
    public static class AsyncMessageSubscribe_doActionCallable implements Callable<AsyncResult<String>> {
        private AsyncMessageSubscribe target;
        private String param1;
        private String param2;

        public AsyncMessageSubscribe_doActionCallable(AsyncMessageSubscribe var1, String var2, String var3) {
            this.target = var1;
            this.param1 = var2;
            this.param2 = var3;
        }

        public AsyncResult<String> call() throws Exception {
            return this.target.doAction(this.param1, this.param2);
        }
    }

    @Test
    public void test2() throws ExecutionException, InterruptedException {
        AsyncMessageSubscribe_doActionCallable callable = new AsyncMessageSubscribe_doActionCallable(impl, "wujiuye", "hello");
        Future result = executorService.submit(callable);
        AsyncResult<String> asyncResult = AsyncResult.newAsyncResultProxy(result);
        System.out.println(asyncResult.get());
    }

}
```

### 编写字节码实现泛型接口需要注意的地方

`test2`方法中的代码，对应字节码的实现如下，源码在`FutureFunctionHandler`这个类中。
```java
public class FutureFunctionHandler implements AsyncFunctionHandler{
        /**
             * asyncMethod有返回值，且返回值类型为Future的处理
             *
             * @param classWriter          类改写器
             * @param interfaceClass       接口
             * @param asyncMethod          异步方法
             * @param proxyObjClass        接口的实现类
             * @param executorServiceClass 线程池的类型
             */
        @Override
        public void doOverrideAsyncFunc(ClassWriter classWriter, Class<?> interfaceClass, Method asyncMethod, Class<?> proxyObjClass, Class<? extends ExecutorService> executorServiceClass) {
              ...........
            // invoke submit callable
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, ByteCodeUtils.getProxyClassName(proxyObjClass), "executorService", Type.getDescriptor(executorServiceClass));
            methodVisitor.visitVarInsn(ALOAD, index);
            if (!executorServiceClass.isInterface()) {
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, executorServiceClass.getName().replace(".", "/"),
                        "submit", ByteCodeUtils.getFuncDesc(Future.class, Callable.class), false);
            } else {
                methodVisitor.visitMethodInsn(INVOKEINTERFACE, executorServiceClass.getName().replace(".", "/"),
                        "submit", ByteCodeUtils.getFuncDesc(Future.class, Callable.class), true);
            }
            // 将返回值存到操作数栈
            methodVisitor.visitVarInsn(ASTORE, ++index);

            // 再来一层代理，对外部屏蔽线程阻塞等待
            methodVisitor.visitVarInsn(ALOAD, index);
            methodVisitor.visitMethodInsn(INVOKESTATIC, AsyncResult.class.getName().replace(".", "/"),
                    "newAsyncResultProxy", ByteCodeUtils.getFuncDesc(AsyncResult.class, Future.class),
                    false);

            methodVisitor.visitInsn(ARETURN);
            ..............
        }
}
```

线程池在调用`AsyncMessageSubscribe_doActionCallable`这个`Callable`的时候，它查找的方法是
```text
java/util/concurrent/Callable.call:()Ljava.lang.Object;
```
因为`Callable`是个泛型接口，如果把实现类的签名和`call`方法的签名改为下面这样反而不行。
```text
类的签名：Ljava/lang/Object;Ljava/util/concurrent/Callable<Lcom/wujiuye/asyncframework/handler/async/AsyncResult<Ljava/lang/String;>;>;"
call方法的签名：()Lcom/wujiuye/asyncframework/handler/async/AsyncResult<Ljava/lang/String;>;
```
因为泛型`<T>`编译后的方法描述符，其实是`()Ljava.lang.Object;`。

如`AsyncResult`泛型类。(选部分)
```java
public class AsyncResult<T> implements Future<T> {

    private T result;
    
    @Override
    public T get() throws InterruptedException, ExecutionException {
        return result;
    }

}
```

`AsyncResult`泛型类编译后的字节码信息。(选部分)
```text
public class com.wujiuye.asyncframework.handler.async.AsyncResult<T> implements java.util.concurrent.Future<T> {
  private T result;
    descriptor: Ljava/lang/Object;

  public T get() throws java.lang.InterruptedException, java.util.concurrent.ExecutionException;
    descriptor: ()Ljava/lang/Object;
    Code:
       0: aload_0
       1: getfield      #2                  // Field result:Ljava/lang/Object;
       4: areturn

```
类型`T`的`descriptor`为`Ljava/lang/Object;`，以及`get`方法中，`getfield`指令指定的类型描述符也是`Ljava/lang/Object;`。


`Callable`接口也是泛型接口，编译后`call`方法的描述符便是`()Ljava.lang.Object;`。
```text
@FunctionalInterface
public interface Callable<V> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    V call() throws Exception;
}
```

所以，如果通过字节码实现`Callable`接口，`call`方法不要设置方法签名，设置方法签名意味着也要改变方法的描述符，一改变就会导致线程池中调用这个`Callable`的`call`方法抛出抽象方法调用错误，原因是根据`Callable`接口的`call`方法的描述符在其实现类中找不到对应的`call`方法。
