package promise

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

class Promise {

    private var status: Status = Status.Pending()

    private val lock = ReentrantReadWriteLock()

    private constructor()

    constructor(executor: (resolve: (Any) -> Unit, reject: (Any) -> Unit) -> Unit) {
        try {
            executor(this::doResolve, this::doReject)
        } catch (t: Throwable) {
            doReject(t)
        }

    }

    private fun updateStatus(newStatus: Status): Collection<QueueItem>? {
        val writeLock = lock.writeLock()
        writeLock.lock()
        log("[${Thread.currentThread()}]$this.updateStatus old state:$status new state:$newStatus")
        val oldStatus = this.status as? Status.Pending
        return if (oldStatus == null) {
            writeLock.unlock()
            null
        } else {
            this.status = newStatus
            writeLock.unlock()
            oldStatus.queue
        }
    }

    private fun doResolve(value: Any) {
        log("[${Thread.currentThread()}]$this.doResolve")
        updateStatus(Status.Fulfilled(value))?.forEach { queueItem ->
            queueItem.doResolve(value)
        }
    }

    private fun doReject(reason: Any) {
        log("[${Thread.currentThread()}]$this.doReject")
        updateStatus(Status.Rejected(reason))?.forEach { queueItem ->
            queueItem.doReject(reason)
        }
    }

    fun then(onFulfilled: (value: Any) -> Any = { it }): Promise = then(onFulfilled) { it }

    fun then(
        onFulfilled: (value: Any) -> Any,
        onRejected: (reason: Any) -> Any
    ): Promise {
        var readLock: Lock? = null
        if (this.status is Status.Pending) {
            readLock = this.lock.readLock()
            readLock.lock()
        }
        log("[${Thread.currentThread()}]$this.then:$status")
        val status = this.status
        return when (status) {
            is Status.Pending -> {
                Promise().also {
                    log("[${Thread.currentThread()}]newPromise$it")
                    status.queue.add(QueueItem(it, onFulfilled, onRejected))
                    readLock?.unlock()
                }
            }
            is Status.Fulfilled -> {
                try {
                    Promise.resolve(onFulfilled(status.value))
                } catch (t: Throwable) {
                    Promise.reject(t)
                }
            }
            is Status.Rejected -> {
                try {
                    Promise.resolve(onRejected(status.reason))
                } catch (t: Throwable) {
                    Promise.reject(t)
                }
            }
        }

    }

    fun catch(onRejected: (reason: Any) -> Any = { it }): Promise = then({ it }, onRejected)

    companion object {
        fun all(iterable: Iterable<Promise>): Promise = Promise { resolve, reject ->
            iterable.forEach { promise ->
                promise.then({
                    if (iterable.all { it.status is Status.Fulfilled }) {
                        resolve(iterable.map { (it.status as Status.Fulfilled).value })
                    }
                }, reject)
            }
        }


        fun race(iterable: Iterable<Promise>): Promise = Promise { resolve, reject ->
            iterable.forEach { promise -> promise.then(resolve, reject) }
        }

        fun resolve(value: Any) = (value as? Promise) ?: Promise { resolve, _ ->
            resolve(value)
        }

        fun reject(reason: Any) = (reason as? Promise) ?: Promise { _, reject ->
            reject(reason)
        }

    }

    private class QueueItem(
        private val outPromise: Promise,
        private val onFulfilled: (Any) -> Any,
        private val onRejected: (Any) -> Any
    ) {
        fun doResolve(value: Any) {
            log("[${Thread.currentThread()}]$this.doResolve")
            notifyChained(
                try {
                    Promise.resolve(onFulfilled(value))
                } catch (t: Throwable) {
                    Promise.reject(t)
                }
            )
        }

        fun doReject(reason: Any) {
            log("[${Thread.currentThread()}]$this.doReject")
            notifyChained(
                try {
                    Promise.reject(onRejected(reason))
                } catch (t: Throwable) {
                    Promise.reject(t)
                }
            )
        }

        private fun notifyChained(result: Promise) {
            log("[${Thread.currentThread()}]$this.notifyChained:$result")
            result.then(outPromise::doResolve, outPromise::doReject)
        }
    }

    private sealed class Status {
        internal data class Pending(val queue: ConcurrentLinkedQueue<QueueItem> = ConcurrentLinkedQueue()) : Status()
        internal data class Fulfilled(val value: Any) : Status()
        internal data class Rejected(val reason: Any) : Status()
    }
}

const val debug = false

private fun log(m: Any) {
    if (debug) println(m)
}