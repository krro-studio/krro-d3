.PHONY: clean compile test jar uberjar repl

all: clean compile-java compile-clj jar install

clean:
	clj -T:build clean

compile-java:
	clj -T:build compile-java

compile-clj:
	clj -T:build compile-clj

jar:
	clj -T:build jar

install:
	clj -T:build install
