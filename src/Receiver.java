import sun.misc.Signal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;

public class Receiver {

    private final ServerSocket serverSocket;
    private final int monitorPid;

    public static void main(String[] args) throws IOException {
        int pid = Integer.parseInt(args[0]);
        new Receiver(pid);
    }

    private Receiver(int monitorPid) throws IOException {
        this.monitorPid = monitorPid;
        serverSocket = new ServerSocket(0);
        System.out.println(serverSocket.getLocalPort());
        Signal.handle(new Signal("INT"), sig -> {
            synchronized (this) {
                notify();
            }
        });
        loop();
    }

    private synchronized void loop() {
        try (var reader = new BufferedReader(new InputStreamReader(serverSocket.accept().getInputStream()))) {
            while (true) {
                final String s = reader.readLine();
                wait();
                System.out.println(s);
                Runtime.getRuntime().exec("kill -SIGINT " + monitorPid);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
