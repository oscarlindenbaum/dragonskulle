/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.Scanner;

/** This is for testing, it creates a CLI client instance and can connect to the server */
public class CreateClient {
    /** The Ip. */
    static String ip;
    /** The Port. */
    static String port;
    /** The Client. */
    static NetworkClient client;
    /** The Command. */
    static String command;

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter IP: ");
        ip = scanner.nextLine();
        System.out.println("Enter Port: ");
        port = scanner.nextLine();
        System.out.println("Creating client");
        ClientListener clientEars = new ClientEars();
        if (ip.equals(port)) {
            client = new NetworkClient("127.0.0.1", 7000, clientEars);
        } else {
            client = new NetworkClient(ip, Integer.parseInt(port), clientEars);
        }
        System.out.println("Commands are (K)ill and (S)end");

        OUTER_LOOP:
        while (true) {
            System.out.println("Enter Command: ");
            command = scanner.nextLine();
            switch (command.toUpperCase()) {
                case ("K"):
                    System.out.println("Killing Client");
                    client.dispose();
                    break OUTER_LOOP;
                case ("S"):
                    System.out.println("Sending Message to server");
                    client.send("Message");
                    break;
                default:
            }
        }
    }
}
