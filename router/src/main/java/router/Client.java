package router;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private final String macAddress;
    private final String ipAddress;
    private final InetAddress routerAddress;
    private final DatagramSocket socket;
    private final int routerPort;
    private final Thread thread;
    private boolean isRunning = true;

    public Client(String macAddress, String ipAddress, String routerHost, int routerPort) throws SocketException, UnknownHostException {
        this.socket = new DatagramSocket();
        this.routerAddress = InetAddress.getByName(routerHost);
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.routerPort = routerPort;
        this.thread = new Thread(this::listenForResponses);
        this.thread.start();
        registerWithRouter();
    }

    private void listenForResponses() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (isRunning && !socket.isClosed()) {
            try {
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                handleReceivedMessage(received);
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error receiving packet: " + e.getMessage());
                }
            }
        }
    }

    private void handleReceivedMessage(String message) {
        String[] parts = message.split(";");
        String command = parts[1];
        if ("PING".equals(command)) {
            String senderMac = parts[0];
            String targetIp = parts[2];
            System.out.println("\nReceived PING from " + senderMac + " (IP: " + targetIp + ")");
            String response = macAddress + ";PONG;" + senderMac;
            sendToRouter(response);
        } else if ("PONG".equals(command)) {
            String senderMac = parts[0];
            System.out.println("Received PONG from " + senderMac);
        }
    }

    public void ping(String targetIp) {
        String message = macAddress + ";PING;" + targetIp;
        sendToRouter(message);
        System.out.println("Sent PING to " + targetIp);
    }

    private void registerWithRouter() {
        String message = macAddress + ";REGISTER;" + ipAddress;
        sendToRouter(message);
        System.out.println("Registered with router: MAC=" + macAddress + ", IP=" + ipAddress);
    }

    private void sendToRouter(String message) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, routerAddress, routerPort);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending to router: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;
        thread.interrupt();
        socket.close();
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println("Usage: java Client <MAC> <IP> [router_host] [router_port]");
                return;
            }
            String mac = args[0];
            String ip = args[1];
            String routerHost = args.length > 2 ? args[2] : "localhost";
            int routerPort = args.length > 3 ? Integer.parseInt(args[3]) : 5000;
            Client client = new Client(mac, ip, routerHost, routerPort);

            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter IP to ping (or 'exit' to exit): ");
            while (true) {
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) {
                    break;
                }
                client.ping(input);
            }
            client.stop();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
