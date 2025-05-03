
package router;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private final String macAddress;
    private String ipAddress;
    private final InetAddress routerAddress;
    private final DatagramSocket socket;
    private final int routerPort;
    private final Thread thread;
    private boolean isRunning = true;
    private boolean registered = false;

    public Client(String macAddress, String routerHost, int routerPort) throws SocketException, UnknownHostException {
        this.socket = new DatagramSocket();
        this.routerAddress = InetAddress.getByName(routerHost);
        this.macAddress = macAddress;
        this.routerPort = routerPort;
        this.thread = new Thread(this::listenForResponses);
        this.thread.start();
        dhcpDiscover();
    }

    // DHCP
    private void dhcpDiscover() {
        try {
            String discoverMessage = macAddress + ";DHCP_DISCOVER";
            byte[] buffer = discoverMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName("255.255.255.255"), 67);
            socket.send(packet);
            System.out.println("Sent DHCP_DISCOVER");
        } catch (IOException e) {
            System.err.println("Failed to send DHCP_DISCOVER");
        }
    }

    private void dhcpRequest(String ipAddress, String DHCPip) {
        try {
            String discoverMessage = macAddress + ";DHCP_REQUEST;" + ipAddress + ";" + DHCPip;
            byte[] buffer = discoverMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName("255.255.255.255"), 67);
            socket.send(packet);
            System.out.println("Sent DHCP_REQUEST");
        } catch (IOException e) {
            System.err.println("Failed to send DHCP_REQUEST");
        }
    }

    private void listenForResponses() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (isRunning && !socket.isClosed()) {
            try {
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received: " + received);
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
        if (parts.length < 2) {
            System.err.println("Invalid message received: " + message);
        }
        String command = parts[1];
        if ("PING".equals(command)) {
            String senderMac = parts[0];
            String targetIp = parts[2];
            String response = macAddress + ";PONG;" + senderMac;
            sendToRouter(response);
            System.out.println("Sent PONG to " + targetIp);
        } else if ("DHCP_OFFER".equals(command)) {
            if (parts.length != 4) {
                System.err.println("Invalid DHCP_OFFER: " + message);
            }
            dhcpRequest(parts[2], parts[3]);
        } else if ("DHCP_ACK".equals(command)) {
            this.ipAddress = parts[2];
            System.out.println("Obtained IP from DHCP: " + ipAddress);
            registerWithRouter();
        } else if ("REGISTER_ACK".equals(command)) {
            System.out.println("Registered with router: MAC=" + macAddress + ", IP=" + ipAddress);
            registered = true;
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
            if (args.length < 1) {
                System.out.println("Usage: java Client <MAC> [router_host] [router_port]");
                return;
            }
            String mac = args[0];
            String routerHost = args.length > 2 ? args[2] : "localhost";
            int routerPort = args.length > 3 ? Integer.parseInt(args[3]) : 5000;
            Client client = new Client(mac, routerHost, routerPort);

            Scanner scanner = new Scanner(System.in);

            while (true) {
                String input = scanner.nextLine();
                if (!client.registered) {
                    System.out.println("Registration failed");
                    break;
                }
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
