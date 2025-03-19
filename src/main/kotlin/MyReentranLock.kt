package org.example
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport

class MyReentranLock (private val isFair: Boolean = false) {
    private var state = AtomicInteger(0)
    private var owner: Thread? = null

    // 头节点 尾节点
    private var dummyHead: AtomicReference<Node> = AtomicReference<Node>(Node())
    private var dummyTail: AtomicReference<Node> = AtomicReference<Node>(dummyHead.get())

    fun lock() {

        if (!isFair) {
            if (state.compareAndSet(0, 1)) {
                println("${Thread.currentThread().name} 直接拿到锁")
                owner = Thread.currentThread()
                return
            }
        }

        if(Thread.currentThread() == owner){
            if ((state.get()>0)) {
                state.incrementAndGet()
                println("${Thread.currentThread().name} 重入锁 ${state.get()}")
                return
            }
        }

        // 没有拿到锁, 进入链表
        val current = Node()
        current.thread = Thread.currentThread()

        while (true) {
            // 此处可能有多线程竞争, 所以循环中每次都要重新获得当前的尾节点
            val curTail = dummyTail.get()
            if (dummyTail.compareAndSet(curTail, current)) {
                println("${Thread.currentThread().name} 加入到链尾")
                current.pre = curTail
                curTail.next = current
                break
            }
        }

        //
        while (true) {
            // 先尝试获得锁, 不行就阻塞 等待别人唤醒
            if (current.pre == dummyHead.get() && state.compareAndSet(0, 1)) {
                owner = Thread.currentThread()
                dummyHead.set(current)
                current.pre?.next = null
                current.pre = null
                println("${Thread.currentThread().name} 被唤醒后拿到锁")
                return
            }
            LockSupport.park()
        }
    }

    fun unlock() {
        // 未持有锁 直接抛异常
        if (Thread.currentThread() != owner) {
            throw IllegalStateException("当前线程没有锁, 不可解锁")
        }
        if(state.get()>1){
            state.getAndDecrement()
            println("${Thread.currentThread().name} 释放重入锁 剩余次数: ${state.get()}")
            return
        }

        val next = dummyHead.get().next

        state.set(0)

        next?.let {
            println("${Thread.currentThread().name} 唤醒了 ${next.thread?.name}")
            LockSupport.unpark(it.thread)
        }
    }

    inner class Node {
        var pre: Node? = null
        var next: Node? = null
        var thread: Thread? = null
    }
}