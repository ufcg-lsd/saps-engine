import math
import sys
import time
import random


def is_prime(n):
    if n % 2 == 0 and n > 2: 
        return False
    for i in range(3, int(math.sqrt(n)) + 1, 2):
        if n % i == 0:
            return False
    return True

def find_primes(i, f):
	primes = []
	for p in range (i, f):
		if is_prime(p):
			primes.append(p)

	f = open("/tmp/primeresult", "w")
	f.write(str(primes))
	f.close()

def failjob(c):
	if (random.randint(0,100) > c:
		return 1 / 0;


failjob(sys.argv[3])
find_primes(int(sys.argv[1]), int(sys.argv[2]))

