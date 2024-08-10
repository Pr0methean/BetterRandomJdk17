# BetterRandomJdk17

**Notice: This library is considered obsolete on [Linux 5.17 and newer](https://www.phoronix.com/news/Linux-getrandom-8450p) because each CPU core now has its own entropy pool, and the overhead of a system call is now at least as cheap as a communication between threads.**

Continuous reseeding via ring buffer for RandomGenerator instances
