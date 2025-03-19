import org.example.MyReentrantLock

fun main() {
    val count = intArrayOf(36)
    val threads = ArrayList<Thread>()
    val lock = MyReentrantLock(true)

    for (i in 0..5) {
        threads.add(Thread {
            for (j in 0..5) {
                lock.lock()
                count[0]--
            }
            for (j in 0..5) {
                lock.unlock()
            }
        })
    }
    threads.forEach { it.start() }
    threads.forEach { it.join() }
    println(count[0])
}