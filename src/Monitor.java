import sun.misc.Signal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Monitor {

    private final Process receiver;
    private final Process sender;
    private volatile boolean senderSignal = true;
    private BufferedReader reader;

    public static void main(String[] args) throws IOException {
        String receiverCommand = args[0];
        String senderCommand = args[1];
        new Monitor(receiverCommand, senderCommand);
    }

    public Monitor(String receiverName, String senderName) throws IOException {
        final long currentPid = ProcessHandle.current().pid();
        log("starting " + receiverName);
        receiver = new ProcessBuilder()
                .command("java", receiverName, String.valueOf(currentPid))
                .start();
        log(receiverName + " started. pid: " + receiver.pid());
        reader = new BufferedReader(new InputStreamReader(receiver.getInputStream()));
        final String receiverPort = Optional.ofNullable(readFromReceiver())
                .orElseThrow();
        log(receiverName + " port: " + receiverPort);
        log("starting " + senderName);
        sender = new ProcessBuilder()
                .command("java", senderName, String.valueOf(currentPid), receiverPort)
                .start();
        log(senderName + " started. pid: " + sender.pid());
        Signal.handle(new Signal("INT"), sig -> {
            synchronized (this) {
                notify();
            }
        });
        log("Monitor started. pid: " + currentPid);
        loop();
    }

    private synchronized void loop() {
        while (true) {
            try {
                if (checkQuit()) break;
                if (senderSignal) {
                    invokeReceiver();
                    wait();
                    log(readFromReceiver());
                    senderSignal = false;
                    TimeUnit.SECONDS.sleep(1);
                } else {
                    invokeSender();
                    wait();
                }
                log("waiting...");
            } catch (Exception ignored) {
            }
        }
    }

    private void log(String message) {
        System.out.println(message);
    }

    private void invokeSender() throws IOException {
        log("sending signal to sender");
        Runtime.getRuntime().exec("kill -SIGINT " + sender.pid());
        senderSignal = true;
    }

    private void invokeReceiver() throws IOException {
        log("sending signal to receiver");
        Runtime.getRuntime().exec("kill -SIGINT " + receiver.pid());
    }

    private boolean checkQuit() throws IOException, InterruptedException {
        if (System.in.available() > 0) {
            log("input available");
            final String input = new Scanner(System.in).next();
            if ("q".equals(input) || "Q".equals(input)) {
                log("killing processes");
                Runtime.getRuntime().exec("kill -SIGTERM " + sender.pid()).waitFor();
                Runtime.getRuntime().exec("kill -SIGTERM " + receiver.pid()).waitFor();
                log("processes killed. terminating...");
                return true;
            }
        }
        return false;
    }

    private String readFromReceiver() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

}
