package router;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DHCPServer {
    private static final int DHCP_PORT = 67;
    private static final String DHCP_SERVER_IP = "192.168.1.1";
    private static final String IP_POOL_START = "192.168.1.2";
    private static final String IP_POOL_END = "192.168.1.254";

    private final Map<String, String> macToIpMap = new ConcurrentHashMap<>(); // MAC -> IP
    private final Map<String, Boolean> ipAvailability = new ConcurrentHashMap<>();
    private volatile boolean isRunning = true;

    public DHCPServer() {
        String[] startParts = IP_POOL_START.split("\\.");
        String[] endParts = IP_POOL_END.split("\\.");

        int start = Integer.parseInt(startParts[3]);
        int end = Integer.parseInt(endParts[3]);

        for (int i = start; i <= end; i++) {
            String ip = startParts[0] + "." + startParts[1] + "." + startParts[2] + "." + i;
            ipAvailability.put(ip, true);
        }
    }

    public void start() {
        try (DatagramSocket socket = new DatagramSocket(DHCP_PORT)) {
            System.out.println("DHCP Server started on port " + DHCP_PORT);

            while (isRunning) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                handleDHCPRequest(socket, packet);
            }
        } catch (IOException e) {
            System.err.println("DHCP Server error: " + e.getMessage());
        }
    }

    private void handleDHCPRequest(DatagramSocket socket, DatagramPacket packet) {
        try {
            String message = new String(packet.getData(), 0, packet.getLength()).trim();
            String[] parts = message.split(";");
            if (parts.length < 2) {
                System.err.println("DHCP Server: Invalid DHCP message: " + message);
                return;
            }

            String macAddress = parts[0];
            String command = parts[1];
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();

            if ("DHCP_DISCOVER".equals(command)) {
                // Check if mac is correct
                if (!macAddress.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) {
                    System.out.println("Invalid MAC format: " + macAddress);
                    return;
                }
                //Check if client already has an IP
                if (macToIpMap.containsKey(macAddress)) {
                    String assignedIp = macToIpMap.get(macAddress);
                    sendDHCPResponse(socket, clientAddress, clientPort, macAddress, assignedIp, "DHCP_ACK");
                    return;
                }

                //Find available IP
                String availableIp = findAvailableIp();
                if (availableIp == null) {
                    System.out.println("No available IP addresses in pool");
                    return;
                }
                sendDHCPResponse(socket, clientAddress, clientPort, macAddress, availableIp, "DHCP_OFFER");
            } else if ("DHCP_REQUEST".equals(command)) {
                if (parts.length < 4) {
                    System.err.println("DHCP Server: Invalid DHCP message: " + message);
                    return;
                }
                if (!parts[3].equals(DHCP_SERVER_IP)) {
                    return;
                }
                //Assign IP
                String ip = parts[2];
                if (!ipAvailability.get(ip)) {
                    System.out.println("Ip is not available.");
                    return;
                }
                macToIpMap.put(macAddress, ip);
                ipAvailability.put(ip, false);
                System.out.println("Assigned IP " + ip + " to MAC " + macAddress);
                sendDHCPResponse(socket, clientAddress, clientPort, macAddress, ip, "DHCP_ACK");
            }
        } catch (Exception e) {
            System.err.println("Error handling DHCP request: " + e.getMessage());
        }
    }

    private String findAvailableIp() {
        for (Map.Entry<String, Boolean> entry : ipAvailability.entrySet()) {
            if (entry.getValue()) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void sendDHCPResponse(DatagramSocket socket, InetAddress clientAddress, int clientPort,
                                  String macAddress, String ipAddress, String responseType) throws IOException {
        String response = macAddress + ";" + responseType + ";" + ipAddress + ";" + DHCP_SERVER_IP;
        byte[] buffer = response.getBytes();
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
        socket.send(responsePacket);
    }

    public void stop() {
        isRunning = false;
    }

    public static void main(String[] args) {
        DHCPServer server = new DHCPServer();
        try {
            server.start();
        } finally {
            server.stop();
        }
    }
}