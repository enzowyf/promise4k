# [promise4k](https://github.com/enzowyf/promise4k)
[![](https://jitpack.io/v/enzowyf/promise4k.svg)](https://jitpack.io/#enzowyf/promise4k)

A promise implementation for Kotlin/JVM. The Api is exactly the same as Promise in JavaScript.

## Gradle
```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
    
dependencies {
    compile 'com.github.enzowyf:promise4k:1.0.0'
}
```
     
## Syntax
To learn about promises, read [Using promises](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Using_promises) first.

### Constructor
```kotlin
val promise = Promise { resolve, reject ->
    setTimeout({
        resolve("foo")
    }, 1000)
}
```

### resolve
```kotlin
Promise.resolve("foo").then { value ->
    println("Get some data:$value")
}
```
### reject
```kotlin
Promise.reject("foo").catch { reason ->
    println("Something wrong:$reason")
}
```
### then
```kotlin
Promise.resolve("foo").then { value ->
    println("Get some data:$value")
}
```
or

```kotlin	
Promise.resolve("foo")
    .then({ value ->
        println("Get some data:$value")
    }, { reason ->
        println("Something wrong:$reason")
    })
```

### catch
```kotlin
Promise.resolve("foo")
    .then { value ->
        println("Get some data:$value")
    }.catch { reason ->
        println("Something wrong:$reason")
    }	
```

### all
```kotlin
Promise.all(listOf(promise1, promise2, promise3)).then {
    println("All finish:$it")
}
```

### race
```kotlin
Promise.race(listOf(promise1, promise2, promise3)).then {
    println("Race winner:$it")
}
```
### Chaining
```kotlin
val p1 = Promise.resolve("foo")
val p2 = Promise.resolve("bar")

p1.then {
    println(it)
    p2
}.then {
    println(it)
    "Hello"
}.then {
    println(it)
    Promise.reject("Oh, no!")
}.catch {
    println(it)
    "Claim down"
}.then {
    println(it)
    throw Exception("Oh, my God!!!")
}.catch {
    println((it as? Exception)?.message)
}
```
