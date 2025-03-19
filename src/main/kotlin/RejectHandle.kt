package org.example

interface RejectHandle {
    fun reject(rejectCommand:Runnable, threadPool: MyThreadPool)
}