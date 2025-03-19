package org.example

class ThrowRejectHandle:RejectHandle {
    override fun reject(rejectCommand: Runnable, threadPool: MyThreadPool) {
        throw RuntimeException("阻塞队列满了!!")
    }
}