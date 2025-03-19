package org.example

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MyThreadPool(
    private val corePoolSize: Int, private val maxSize: Int,
    private val timeOut: Long, private val timeUnit: TimeUnit,
    private val blockingQueue: ArrayBlockingQueue<Runnable>,
    private val rejectHandle: RejectHandle
) {
    private val threadList = ArrayList<Thread>()

    private var atomicInt = AtomicInteger()
    private val mainLock = MyReentranLock()

    private var isShutdown = false


    // 线程池利用池化思想, 复用已经创建的线程
    fun execute(task: Runnable) {
        mainLock.lock()
        try {
            // 如果该进程池已经被停止, 那么拒绝任务
            if (isShutdown) {
                rejectHandle.reject(task, this)
                return
            }

            // 1. 创建核心线程
            if (threadList.size < corePoolSize) {
                val thread = PooledThread(task, true)
                threadList.add(thread)
                thread.start()
                return
            }

            // 2. 核心线程满了 尝试入队
            if (blockingQueue.offer(task)) return

            // 3. 队列满了 创建辅助线程
            var isRun = false
            // 辅助线程
            if (threadList.size < maxSize) {

                // 如果还有辅助线程的额度, 则直接把该任务交给辅助线程
                val thread = PooledThread(task, false)
                threadList.add(thread)
                thread.start()
                isRun = true
            }
            // 4. 全部都满了 执行拒绝策略
            if (!isRun && !blockingQueue.offer(task)) {
                rejectHandle.reject(task, this)
            }
        } finally {
            mainLock.unlock()
        }
    }

    fun shutdown() {
        mainLock.lock()
        try {
            isShutdown = true
            threadList.forEach {
                it.interrupt()
            }
        } finally {
            mainLock.unlock()
            println("全部结束!")
        }
    }

    //TODO  ThreadFactory参数有什么用

    inner class PooledThread(private val firstTask: Runnable?, private val isCore: Boolean) : Thread() {
        private var isFirst = true

        init {
            name = if (isCore) "核心线程" else "辅助线程"
            name += atomicInt.getAndIncrement()
        }

        override fun run() {
            while (!isInterrupted) {
                try {
                    val cmd = if (isFirst && firstTask != null) {
                        isFirst = false
                        firstTask
                    } else {
                        if (isCore) {
                            blockingQueue.take()
                        } else {
                            blockingQueue.poll(timeOut, timeUnit) ?: break
                        }
                    }
                    cmd.run()
                } catch (e: InterruptedException) {
                    println("${currentThread().name} 被中断! 当前线程池还有${threadList.size}个线程")
                    interrupt()
                    break
                }
            }


            // 退出循环后
            mainLock.lock()
            try {
                threadList.remove(this)
                println("正在将停止工作的 $name 移出...还剩 ${threadList.size} 个线程!")
                atomicInt.getAndDecrement()
            } finally {
                mainLock.unlock()
            }
        }
    }
}