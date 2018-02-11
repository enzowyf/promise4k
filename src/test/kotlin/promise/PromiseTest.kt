package promise

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
                println("Get some data:$value")
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
            }, { reason ->
                println("Something wrong:$reason")
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
                println("Something wrong:$reason")
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
                    println("Something wrong:$reason")
                }
        }
    }

    @Test
    fun resolve() {
        Promise.resolve("foo").then { value ->
            println("Get some data:$value")
        }
    }

    @Test
    fun reject() {
        Promise.reject("foo").catch { reason ->
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
                println("All finish:$it")
            }.catch {
                    println("Something wrong:$it")
                }

            Promise.all(listOf(promise1, promise2, promise4)).then {
                println("All finish:$it")
            }.catch {
                    println("Something wrong:$it")
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
                println("Race winner:$it")
            }.catch {
                    println("Something wrong:$it")
                }

            Promise.race(listOf(promise1, promise2, promise4)).then {
                println("Race winner:$it")
            }.catch {
                    println("Something wrong:$it")
                }
        }
    }

    @Test
    fun testChain() {
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
                println(it)
                promise2
            }.then {
                    println(it)
                    promise3
                }.catch {
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