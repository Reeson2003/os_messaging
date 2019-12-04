import sun.misc.Signal;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;

public class Sender {

    private int monitorPid;
    private Socket socket;

    public static void main(String[] args) throws IOException {
        int pid = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        new Sender(pid, port);
    }

    public Sender(int monitorPid, int port) throws IOException {
        this.monitorPid = monitorPid;
        socket = new Socket("localhost", port);
        Signal.handle(new Signal("INT"), sig -> {
            synchronized (this) {
                notify();
            }
        });
        loop();
    }

    public synchronized void loop() {
        try (var writer = new PrintWriter(socket.getOutputStream())) {
            while (true) {
                writer.println(Instant.now().toString());
                writer.flush();
                Runtime.getRuntime().exec("kill -SIGINT " + monitorPid);
                wait();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
