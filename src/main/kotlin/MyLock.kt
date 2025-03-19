package org.example

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport

class MyLock(private val isFair: Boolean = false) {
    private var flag = AtomicBoolean(false)
    private var owner: Thread? = null

    // 头节点 尾节点
    private var dummyHead: AtomicReference<Node> = AtomicReference<Node>(Node())
    private var dummyTail: AtomicReference<Node> = AtomicReference<Node>(dummyHead.get())

    fun lock() {
        if (!isFair) {
            if (flag.compareAndSet(false, true)) {
                println("${Thread.currentThread().name} 直接拿到锁")
                owner = Thread.currentThread()
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
            if (current.pre == dummyHead.get() && flag.compareAndSet(false, true)) {
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

        val next = dummyHead.get().next

        flag.set(false)

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