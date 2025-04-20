package router;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Router {
    private static final int PORT = 5000;
    private volatile boolean isRunning = true;
    private final Map<String, String> arpTable = new ConcurrentHashMap<>(); // IP -> MAC
    private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>(); // MAC -> address
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        Router router = new Router();
        try {
            router.start();
        } finally {
            router.stop();
        }
    }

    public void start() {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("Router started on port " + PORT);

            while (isRunning) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                executor.execute(() -> handlePacket(socket, packet));
            }
        } catch (IOException e) {
            System.err.println("Router error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    private void handlePacket(DatagramSocket socket, DatagramPacket packet) {
        try {
            String message = new String(packet.getData()).trim();
            String[] parts = message.split(";");
            String senderMac = parts[0];
            String command = parts[1];

            ClientInfo info = clients.get(senderMac);
            if (info == null) {
                clients.put(senderMac, new ClientInfo(null, new InetSocketAddress(packet.getAddress(), packet.getPort())));
            } else {
                clients.put(senderMac, new ClientInfo(info.ip, new InetSocketAddress(packet.getAddress(), packet.getPort())));
            }

            switch (command) {
                case "REGISTER":
                    String ip = parts[2];
                    handleRegistration(senderMac, ip, socket);
                    break;
                case "PING":
                    String targetIp = parts[2];
                    handlePing(socket, senderMac, targetIp);
                    break;
                case "PONG":
                    String targetMac = parts[2];
                    forwardPong(socket, targetMac, message);
                    break;
                default:
                    System.out.println("Unknown command: " + command);
            }
        } catch (Exception e) {
            System.err.println("Error handling packet: " + e.getMessage());
        }
    }

    private void handleRegistration(String mac, String ip, DatagramSocket socket) {
        if (arpTable.containsKey(ip) && !arpTable.get(ip).equals(mac)) {
            System.out.println("IP conflict detected! IP " + ip + " already registered to MAC " + arpTable.get(ip));
            return;
        }

        ClientInfo existingClient = clients.get(mac);
        if (existingClient != null && existingClient.ip != null && !existingClient.ip.equals(ip)) {
            System.out.println("MAC conflict detected! MAC " + mac + " already registered to IP " + existingClient.ip);
            return;
        }

        if (!mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) {
            System.out.println("Invalid MAC format: " + mac);
            return;
        }

        if (!ip.matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$")) {
            System.out.println("Invalid IP format: " + ip);
            return;
        }

        arpTable.put(ip, mac);
        clients.compute(mac, (k, v) ->
                v == null ?
                        new ClientInfo(ip, null) :
                        new ClientInfo(ip, v.adress));
        String message = mac + ";" + "REGISTER_ACK" + ";" + ip;
        byte[] buffer = message.getBytes();
        try {
            socket.send(new DatagramPacket(buffer, buffer.length, clients.get(mac).adress));
            System.out.println("Registered: IP " + ip + " -> MAC " + mac);
        } catch (IOException e) {
            System.err.println("Error handling registration: " + e.getMessage());
        }
    }

    private void handlePing(DatagramSocket socket, String senderMac, String targetIp) {
        if (!arpTable.containsKey(targetIp)) {
            System.out.println("IP " + targetIp + " not found in routing table");
            return;
        }

        String targetMac = arpTable.get(targetIp);
        ClientInfo targetInfo = clients.get(targetMac);

        if (!clients.containsKey(targetMac)) {
            System.out.println("MAC " + targetMac + " not connected");
            return;
        }

        String message = senderMac + ";PING;" + targetIp;
        byte[] buffer = message.getBytes();

        try {
            socket.send(new DatagramPacket(buffer, buffer.length, targetInfo.adress));
            System.out.println("Forwarded PING from " + senderMac + " to " + targetMac);
        } catch (IOException e) {
            System.err.println("Error forwarding PING: " + e.getMessage());
        }
    }

    private void forwardPong(DatagramSocket socket, String targetMac, String message) {
        if (!clients.containsKey(targetMac)) {
            System.out.println("MAC " + targetMac + " not connected for PONG");
            return;
        }

        byte[] buffer = message.getBytes();
        ClientInfo targetInfo = clients.get(targetMac);
        try {
            socket.send(new DatagramPacket(buffer, buffer.length, targetInfo.adress));
            System.out.println("Forwarded PONG to " + targetMac);
        } catch (IOException e) {
            System.err.println("Error forwarding PONG: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;
        executor.shutdown();
    }
}
