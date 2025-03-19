package org.example

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 线程池 简单实现
 *
 */
fun main() {
    println("Hello World!")
    val myThreadPool = MyThreadPool(2, 7, 1, TimeUnit.SECONDS,
        ArrayBlockingQueue(15), ThrowRejectHandle()
    )
    val runnable = Runnable {
        Thread.sleep(1000)
        println("${Thread.currentThread().name} at main")
    }

    for (i in 0..19) {
        myThreadPool.execute(runnable)
    }

    println("主线程没有被阻塞!")
    Thread.sleep(10000)
    println("主线程睡眠结束!")
    for (i in 0..19) {
        myThreadPool.execute(runnable)
    }

    Thread.sleep(10000)
    myThreadPool.shutdown()
}