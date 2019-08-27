package promise

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class PromiseTest {

    @Test
    fun then() {
        countDown { count ->
            val promise = Promise { resolve, reject ->
                setTimeout({
                    resolve("foo")
                    count.countDown()
                }, 1000)
            }

            promise.then { value ->
                Assert.assertEquals("foo", value)
                println("Get some data:$value")
            }.catch {
                println("Something wrong:$it")
            }
        }
    }

    @Test
    fun then1() {
        countDown { count ->
            val promise1 = Promise { resolve, reject ->
                setTimeout({
                    println("${Thread.currentThread()} running")
                    resolve("foo")
                }, 1000)
            }

            promise1.then({ value ->
                println("Get some data:$value")
                Assert.assertEquals("foo", value)
            }, { reason ->
                println("promise1 go wrong:$reason")
            })

            val promise2 = Promise { resolve, reject ->
                setTimeout({
                    println("${Thread.currentThread()} running")
                    reject("bar")
                    count.countDown()
                }, 1000)
            }

            promise2.then({ value ->
                println("Get some data:$value")
            }, { reason ->
                println("promise2 go  wrong:$reason")
                Assert.assertEquals("bar", reason)
            })
        }
    }

    @Test
    fun catch() {
        countDown { count ->
            val promise = Promise { resolve, reject ->
                setTimeout({
                    println("${Thread.currentThread()} running")
                    reject("bar")
                    count.countDown()
                }, 1000)
            }

            promise.then { value ->
                println("Get some data:$value")
            }.catch { reason ->
                Assert.assertEquals("bar", reason)
                println("Something wrong:$reason")
            }
        }
    }

    @Test
    fun resolve() {
        Promise.resolve("foo").then { value ->
            Assert.assertEquals("foo", value)
            println("Get some data:$value")
        }
    }

    @Test
    fun reject() {
        Promise.reject("foo").catch { reason ->
            Assert.assertEquals("foo", reason)
            println("Something wrong:$reason")
        }
    }

    @Test
    fun all() {
        countDown { count ->
            val promise1 = Promise { resolve, reject ->
                setTimeout({
                    resolve("foo")
                }, 1000)
            }

            val promise2 = Promise { resolve, reject ->
                setTimeout({
                    resolve("bar")
                }, 2000)
            }

            val promise3 = Promise { resolve, reject ->
                setTimeout({
                    resolve("123")
                    count.countDown()
                }, 3000)
            }

            val promise4 = Promise { resolve, reject ->
                setTimeout({
                    reject("456")
                }, 2000)
            }

            Promise.all(listOf(promise1, promise2, promise3)).then {
                Assert.assertArrayEquals(arrayOf("foo", "bar", "123"), it as Array<*>)
                println("All finish in test1:$it")
            }.catch {
                println("Something wrong in test1:$it")
            }

            Promise.all(listOf(promise1, promise2, promise4)).then {
                println("All finish in test2:$it")
            }.catch {
                Assert.assertEquals("456", it)
                println("Something wrong in test2:$it")
            }
        }
    }

    @Test
    fun race() {
        countDown { count ->
            val promise1 = Promise { resolve, reject ->
                setTimeout({
                    resolve("foo")
                }, 1000)
            }

            val promise2 = Promise { resolve, reject ->
                setTimeout({
                    resolve("bar")
                }, 2000)
            }

            val promise3 = Promise { resolve, reject ->
                setTimeout({
                    resolve("123")
                    count.countDown()
                }, 3000)
            }

            val promise4 = Promise { resolve, reject ->
                setTimeout({
                    reject("456")
                }, 500)
            }

            Promise.race(listOf(promise1, promise2, promise3)).then {
                Assert.assertEquals("foo", it)
                println("Race winner in test1:$it")
            }.catch {
                println("Something wrong in test1:$it")
            }

            Promise.race(listOf(promise1, promise2, promise4)).then {
                println("Race winner in test2:$it")
            }.catch {
                Assert.assertEquals("456", it)
                println("Something wrong in test2:$it")
            }
        }
    }

    @Test
    fun testChain() {
        val p1 = Promise.resolve("foo")
        val p2 = Promise.resolve("bar")

        p1.then {
            Assert.assertEquals("foo", it)
            println(it)
            p2
        }.then {
            Assert.assertEquals("bar", it)
            println(it)
            "Hello"
        }.then {
            Assert.assertEquals("Hello", it)
            println(it)
            Promise.reject("Oh, no!")
        }.catch {
            Assert.assertEquals("Oh, no!", it)
            println(it)
            "Claim down"
        }.then {
            Assert.assertEquals("Claim down", it)
            println(it)
            throw Exception("Oh, my God!!!")
        }.catch {
            Assert.assertEquals("Oh, my God!!!", it)
            println((it as? Exception)?.message)
        }
    }

    @Test
    fun testChain1() {
        countDown { count ->
            val promise1 = Promise { resolve, reject ->
                setTimeout({
                    resolve("foo")
                }, 1000)
            }

            val promise2 = Promise { resolve, reject ->
                setTimeout({
                    resolve("bar")
                }, 2000)
            }

            val promise3 = Promise { resolve, reject ->
                setTimeout({
                    reject("123")
                    count.countDown()
                }, 3000)
            }

            promise1.then {
                Assert.assertEquals("foo", it)
                println(it)
                promise2
            }.then {
                Assert.assertEquals("bar", it)
                println(it)
                promise3
            }.catch {
                Assert.assertEquals("123", it)
                println(it)
            }
        }
    }

    init {
    }
}

fun countDown(executor: (count: CountDownLatch) -> Unit) {
    val count = CountDownLatch(1)
    executor(count)
    try {
        count.await()
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
}

fun setTimeout(runnable: () -> Unit, delay: Long) {
    println("setTimeout:$delay")
    val count = CountDownLatch(1)
    thread {
        Thread.sleep(delay)
        runnable()
    }
}