javac -d out -XDignore.symbol.file src/Monitor.java src/Receiver.java src/Sender.java
cd out || exit
java Monitor Receiver Sender