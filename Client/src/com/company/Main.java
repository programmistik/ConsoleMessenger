package com.company;

import java.util.Scanner;
import java.util.*;

public class Main {
    /*
     * To start the Client in console mode use one of the following command
     * > java Client
     * > java Client username
     * > java Client username portNumber
     * > java Client username portNumber serverAddress
     * at the console prompt
     * If the portNumber is not specified 1500 is used
     * If the serverAddress is not specified "localHost" is used
     * If the username is not specified "Anonymous" is used
     */
    public static void main(String[] args) {
        // default values if not entered
        int portNumber = 1500;
        String serverAddress = "localhost";
        String userName = "Anonymous";
        Scanner scan = new Scanner(System.in);

        System.out.println("Enter the username: ");
        userName = scan.nextLine();

        // different case according to the length of the arguments.
        switch(args.length) {
            case 3:
                // for > javac Client username portNumber serverAddr
                serverAddress = args[2];
            case 2:
                // for > javac Client username portNumber
                try {
                    portNumber = Integer.parseInt(args[1]);
                }
                catch(Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
            case 1:
                // for > javac Client username
                userName = args[0];
            case 0:
                // for > java Client
                break;
            // if number of arguments are invalid
            default:
                System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                return;
        }
        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName);
        // try to connect to the server and return if not connected
        if(!client.start())
            return;

        System.out.println("\nHello.! Welcome to the ConsoleChat.");
        printInstructions();

        // infinite loop to get the input from the user
        while(true) {
            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if message is LOGOUT
            String[] w = msg.split(" ",3);
            if(msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                break;
            }
            // message to check who are present in chatroom
            else if(msg.equalsIgnoreCase("WHOISIN")) {
                client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));
            }
            else if(msg.equalsIgnoreCase("HELP")) {
                printInstructions();
            }
            else if(w[0].equalsIgnoreCase("#NEW")) {
                client.sendMessage(new ChatMessage(ChatMessage.NEW, msg));
            }
            else if(w[0].equalsIgnoreCase("#ADD")) {
                client.sendMessage(new ChatMessage(ChatMessage.ADD, msg));
            }
            else if(w[0].equalsIgnoreCase("#DELETE")) {
                client.sendMessage(new ChatMessage(ChatMessage.DELETE, msg));
            }
            else if(w[0].equalsIgnoreCase("#LEAVE")) {
                client.sendMessage(new ChatMessage(ChatMessage.LEAVE, msg));
            }
            else if(w[0].equalsIgnoreCase("#GET")) {
                client.sendMessage(new ChatMessage(ChatMessage.GET, msg));
            }
            // regular text message
            else {
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }
        // close resource
        scan.close();
        // client completed its job. disconnect client.
        client.disconnect();
    }

    public static void printInstructions()
    {
        System.out.println("Instructions:");
        System.out.println("1. Simply type the message to send broadcast to all active clients");
        System.out.println("2. Type '@username<space>yourmessage' without quotes to send message to desired client");
        System.out.println("3. Type 'WHOISIN' without quotes to see list of active clients");
        System.out.println("4. Type 'LOGOUT' without quotes to logoff from server");
        System.out.println("5. Type 'HELP' without quotes to get tis instructions");
        System.out.println("6. Type '#NEW<space>groupname' without quotes to create a new group");
        System.out.println("7. Type '@groupname<space>yourmessage' without quotes to send message to desired group");
        System.out.println("8. Type '#ADD<space>@username<space>@groupname' without quotes to add user to group");
        System.out.println("9. Type '#DELETE<space>@username<space>@groupname' without quotes to delete user from group");
        System.out.println("10. Type '#LEAVE<space>@groupname' without quotes to leave group");
        System.out.println("11. Type '#GET' without quotes to get groupname, all users and their role");
    }
}
