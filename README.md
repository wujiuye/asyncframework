# asyncframework
asm+动态字节码实现的一个异步框架，在你的接口上添加一个@AsyncFunction注解即可让这个方法异步执行，不依赖任何第三方框架！项目长期维护。

###  添加依赖 (gradle版)
```groovy
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
}

dependencies {
    compile group: 'com.github.wujiuye', name: 'asyncframework', version: '1.0.1'
}
```

### 使用介绍

第一步：在接口的需要异步执行的方法上加上@AsyncFunction注解
```java
public interface AsyncMessageSubscribe {

    @AsyncFunction
    void pullMessage(String queue);

}
```

第二步：编写接口的实现类
```java
 AsyncMessageSubscribe impl = (String queue) -> System.out.println(queue);
```
第三步：初始化异步线程池
```java
ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
```

第四步，使用AsmProxyFactory获取代理对象
```java
 public class AsmProxyTest {
 
     @Test
     public void testAutoProxyAsync() throws Exception {
         AsyncMessageSubscribe impl = (String queue) -> System.out.println(queue);
         ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
         AsyncMessageSubscribe proxy = AsmProxyFactory.getInterfaceImplSupporAsync(AsyncMessageSubscribe.class, impl, executorService);
         proxy.pullMessage("wujiuye");
         System.in.read();
     }
 
 }

```