# Promise4K
A promise implementation for Kotlin/JVM. The Api is exactly the same as Promise in JavaScript.

## Syntax
To learn about promises, read [Using promises]([Promise](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Using_promises)) first.

### Constructor
    val promise = Promise { resolve, reject ->
      setTimeout({
        resolve("foo")
      }, 1000)
    }


### resolve
    Promise.resolve("foo").then { value ->
       println("Get some data:$value")
    }

### reject
	Promise.reject("foo").catch { reason ->
      println("Something wrong:$reason")
    }

### then
	Promise.resolve("foo").then { value ->
       println("Get some data:$value")
    }

or
	
	Promise.resolve("foo")
        .then({ value ->
          println("Get some data:$value")
        }, { reason ->
          println("Something wrong:$reason")
        })

### catch
	Promise.resolve("foo")
		.then { value ->
           println("Get some data:$value")
      	}.catch { reason ->
           println("Something wrong:$reason")
    	}	

### all
	Promise.all(listOf(promise1, promise2, promise3)).then {
        println("All finish:$it")
    }

### race

	Promise.race(listOf(promise1, promise2, promise3)).then {
        println("Race winner:$it")
    }

### Chaining

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
