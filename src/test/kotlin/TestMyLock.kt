import org.example.MyLock

fun main() {
    val count = intArrayOf(1000)
    val threads = ArrayList<Thread>()
    val lock = MyLock(true)

    for (i in 0..99) {
        threads.add(Thread {
            lock.lock()
            try {
                println("${Thread.currentThread().name} 正在进行...")
                for (j in 0..9) {
//                    Thread.sleep(2)
                    count[0]--
                }
            } finally {
                lock.unlock()
                println("${Thread.currentThread().name} 执行完毕!")
            }
        })
    }
    threads.forEach { it.start() }
    threads.forEach { it.join() }
    println(count[0])

}