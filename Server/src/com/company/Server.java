package com.company;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

// the server that can be run as a console
public class Server {
    // a unique ID for each connection
    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> al;
    // to display time
    private SimpleDateFormat sdf;
    // the port number to listen for connection
    private int port;
    // to check if server is running
    private boolean keepGoing;
    // notification
    private String notif = " *** ";
    // an ArrayList to keep the list of the Groups
    private ArrayList<Group> groups;

    //constructor that receive the port to listen to for connection as parameter

    public Server(int port) {
        // the port
        this.port = port;
        // to display hh:mm:ss
        sdf = new SimpleDateFormat("HH:mm:ss");
        // an ArrayList to keep the list of the Client
        al = new ArrayList<ClientThread>();
        groups = new ArrayList<Group>();
    }

    public void start() {
        keepGoing = true;
        //create socket server and wait for connection requests
        try
        {
            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections ( till server is active )
            while(keepGoing)
            {
                display("Server waiting for Clients on port " + port + ".");

                // accept connection if requested from client
                Socket socket = serverSocket.accept();
                // break if server stoped
                if(!keepGoing)
                    break;
                // if client is connected, create its thread
                ClientThread t = new ClientThread(socket);
                //add this client to arraylist
                al.add(t);

                t.start();
            }
            // try to stop the server
            try {
                serverSocket.close();
                for(int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        // close all data streams and socket
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    }
                    catch(IOException ioE) {
                    }
                }
            }
            catch(Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        }
        catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    // to stop the server
    protected void stop() {
        keepGoing = false;
        try {
            new Socket("localhost", port);
        }
        catch(Exception e) {
        }
    }

    // Display an event to the console
    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    // to broadcast a message to all Clients
    private synchronized boolean broadcast(String message) {
        // add timestamp to the message
        String time = sdf.format(new Date());

        // to check if message is private i.e. client to client message
        String[] w = message.split(" ",3);

        boolean isPrivate = false;
        if(w[1].charAt(0)=='@')
            isPrivate=true;


        // if private message, send message to mentioned username only
        if(isPrivate==true)
        {
            String tocheck=w[1].substring(1, w[1].length());

            message=w[0]+w[2];
            String messageLf = time + " " + message + "\n";
            boolean found=false;
            // we loop in reverse order to find the mentioned username
            for(int y=al.size(); --y>=0;)
            {
                ClientThread ct1=al.get(y);
                String check=ct1.getUsername();
                if(check.equals(tocheck))
                {
                    // try to write to the Client if it fails remove it from the list
                    if(!ct1.writeMsg(messageLf)) {
                        al.remove(y);
                        display("Disconnected Client " + ct1.username + " removed from list.");
                    }
                    // username found and delivered the message
                    found=true;
                    break;
                }

            }
            // mentioned user not found, return false
            if(found!=true)
            {
                // try if this message is for group
                var GroupIsFind = groups.stream().anyMatch(c ->c.getName().equalsIgnoreCase(tocheck));

                if (GroupIsFind){
                    Group group = groups.stream().filter(c -> c.getName().equalsIgnoreCase(tocheck)).findFirst().get();
                    for (Integer memb: group.getMembers() ) {
                        ClientThread ct = al.stream().filter(c -> c.id == memb).findFirst().get();
                        String mess = time + " " + message + "\n";
                        // display message
                        System.out.print(mess);
                        if(!ct.writeMsg(mess)) {
                            al.remove(memb);
                            display("Disconnected Client " + ct.username + " removed from list.");
                        }
                    }
                    return true;
                }
                ////
                else
                    return false;
            }
        }
        // if message is a broadcast message
        else
        {
            String messageLf = time + " " + message + "\n";
            // display message
            System.out.print(messageLf);

            // we loop in reverse order in case we would have to remove a Client
            // because it has disconnected
            for(int i = al.size(); --i >= 0;) {
                ClientThread ct = al.get(i);
                // try to write to the Client if it fails remove it from the list
                if(!ct.writeMsg(messageLf)) {
                    al.remove(i);
                    display("Disconnected Client " + ct.username + " removed from list.");
                }
            }
        }
        return true;


    }

    // if client sent LOGOUT message to exit
    synchronized void remove(int id) {

        String disconnectedClient = "";
        // scan the array list until we found the Id
        for(int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            // if found remove it
            if(ct.id == id) {
                disconnectedClient = ct.getUsername();
                al.remove(i);
                break;
            }
        }
        broadcast(notif + disconnectedClient + " has left the chat." + notif);
    }

    /*
     *  To run as a console application
     * > java Server
     * > java Server portNumber
     * If the port number is not specified 1500 is used
     */


    // One instance of this thread will run for each client
    class ClientThread extends Thread {
        // the socket to get messages from client
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        // my unique id (easier for deconnection)
        int id;
        // the Username of the Client
        String username;
        // message object to recieve message and its type
        ChatMessage cm;
        // timestamp
        String date;

        // Constructor
        ClientThread(Socket socket) {
            // a unique id
            id = ++uniqueId;
            this.socket = socket;
            //Creating both Data Stream
            System.out.println("Thread trying to create Object Input/Output Streams");
            try
            {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput  = new ObjectInputStream(socket.getInputStream());
                // read the username
                username = (String) sInput.readObject();
                broadcast(notif + username + " has joined the chat room." + notif);
            }
            catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }
            catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        // infinite loop to read and forward message
        public void run() {
            // to loop until LOGOUT
            boolean keepGoing = true;
            while(keepGoing) {
                // read a String (which is an object)
                try {
                    cm = (ChatMessage) sInput.readObject();
                }
                catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                }
                catch(ClassNotFoundException e2) {
                    break;
                }
                // get the message from the ChatMessage object received
                String message = cm.getMessage();

                // different actions based on type message
                switch(cm.getType()) {

                    case ChatMessage.MESSAGE:
                        boolean confirmation =  broadcast(username + ": " + message);
                        if(confirmation==false){
                            String msg = notif + "Sorry. No such user exists." + notif;
                            writeMsg(msg);
                        }
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                        // send list of active clients
                        for(int i = 0; i < al.size(); ++i) {
                            ClientThread ct = al.get(i);
                            writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
                        }
                        break;
                    case ChatMessage.NEW:
                        String[] w = message.split(" ",3);
                        String gName = w[1];
                        boolean isTaken;
                        if (groups.size() == 0)
                            isTaken = false;
                        else
                            isTaken = groups.stream().anyMatch(c ->c.getName().equalsIgnoreCase(gName));
                               // filter(c -> c.getName().equals(gName)).findAny().isPresent();
                        if (isTaken)
                        {
                            display(username + " Name " + gName.toString() + " is already taken");
                            break;
                        }


                        Group newGroup = new Group(gName, id);
                        groups.add(newGroup);
                        display(username + " create new group " + gName.toString());
                        writeMsg("Created!");



                        break;
                    case ChatMessage.ADD:
                        String[] str = message.split(" ",3);
                        String usr = str[1].substring(1, str[1].length());
                        String gr = str[2].substring(1, str[2].length());
                        // only admins can add new users
                        // check if group with this name is in list
                        Boolean grPresent = groups.stream().anyMatch(c ->c.getName().equalsIgnoreCase(gr));

                        if (grPresent) {
                            Boolean usrPresent = al.stream().anyMatch(c ->c.getUsername().equalsIgnoreCase(usr));
                            if (usrPresent){
                                // only admins can add new users
                                Group group = groups.stream().filter(c -> c.getName().equalsIgnoreCase(gr)).findFirst().get();
                                if (group.isAdmin(id)) {
                                    ClientThread ct = al.stream().filter(c -> c.getUsername().equalsIgnoreCase(usr)).findFirst().get();

                                    group.addMember(ct.id);

                                    display(username + " Add user " + usr.toString() + " to group " + gr.toString() );
                                    writeMsg("Done!");
                                    ct.writeMsg(username + " add you to group "  + gr.toString() );
                                    break;
                                }
                                else {
                                    display(username + " Only admins can add users to group "  );
                                    break;
                                }
                            }
                            else{
                                display(username + " There is no user with name " + usr.toString() );
                                break;
                            }
                        }
                        else{
                            display(username + " There is no group with name " + gr.toString() );
                            break;
                        }


                    case ChatMessage.DELETE:
                        String[] str1 = message.split(" ",3);
                        String usr1 = str1[1].substring(1, str1[1].length());
                        String gr1 = str1[2].substring(1, str1[2].length());

                        // check if group with this name is in list
                        Boolean grPresent1 = groups.stream().anyMatch(c ->c.getName().equalsIgnoreCase(gr1));

                        if (grPresent1) {
                            Boolean usrPresent = al.stream().anyMatch(c ->c.getUsername().equalsIgnoreCase(usr1));
                            if (usrPresent){
                                // only admins can delete users
                                Group group = groups.stream().filter(c -> c.getName().equalsIgnoreCase(gr1)).findFirst().get();
                                if (group.isAdmin(id)) {
                                    ClientThread ct = al.stream().filter(c -> c.getUsername().equalsIgnoreCase(usr1)).findFirst().get();

                                    group.deleteMember(ct.id);
                                    writeMsg("Done!");
                                    display(username + " delete user " + usr1.toString() + " from group " + gr1.toString() );

                                    ct.writeMsg(username + " delete you from group "  + gr1.toString() );
                                    break;
                                }
                                else {
                                    writeMsg(" Only admins can delete users from group "  );
                                    break;
                                }
                            }
                            else{
                                display(username + " There is no user with name " + usr1.toString() );
                                break;
                            }
                        }
                        else{
                            display(username + " There is no group with name " + gr1.toString() );
                            break;
                        }
                    case ChatMessage.LEAVE:

                        String[] str2 = message.split(" ",3);

                        String grName = str2[1].substring(1, str2[1].length());
                        // check if group with this name is in list
                        Boolean grPresent2 = groups.stream().anyMatch(c ->c.getName().equalsIgnoreCase(grName));

                        if (grPresent2) {
                            Group group = groups.stream().filter(c -> c.getName().equalsIgnoreCase(grName)).findFirst().get();
                            if(group.isAdmin(id)){
                                // if admin leavs group delete it
                                for (Integer itm:group.getMembers() ) {
                                    ClientThread ct = al.stream().filter(c -> c.id == itm).findFirst().get();
                                    ct.writeMsg(username + " delete group "  + grName.toString() );
                                }
                                groups.remove(group);
                                display(username + " delete group " + grName.toString() );
                                break;
                            }
                            else{
                                group.deleteMember(id);
                                display(username + " leaved group " + grName.toString() );
                                writeMsg("You leaved group "+ grName.toString() );
                                for (Integer itm:group.getMembers() ) {
                                    ClientThread ct = al.stream().filter(c -> c.id == itm).findFirst().get();
                                    ct.writeMsg(username + " leaved group "  + grName.toString() );
                                }
                                break;
                            }
                        }
                        else{
                            display(username + " There is no group with name " + grName.toString() );
                            break;
                        }

                    case ChatMessage.GET:
                        writeMsg(username + " you are member of this groups: "  );
                        Integer counter = 1;
                        for (Group itm: groups ) {
                            if(itm.isMember(id)){
                                writeMsg(counter.toString() + ") " + itm.getName().toUpperCase() + ": ");
                                for (Integer memb: itm.getMembers() ) {
                                    String memberName = al.stream().filter(c -> c.id == memb).findFirst().get().username;
                                    String admin = "";
                                    if(itm.isAdmin(memb)){
                                        admin = " is admnin";
                                    }
                                    writeMsg("- " + memberName + admin);
                                }
                            }
                        }
                        break;
                }
            }
            // if out of the loop then disconnected and remove from client list
            remove(id);
            close();
        }

        // close everything
        private void close() {
            try {
                if(sOutput != null) sOutput.close();
            }
            catch(Exception e) {}
            try {
                if(sInput != null) sInput.close();
            }
            catch(Exception e) {};
            try {
                if(socket != null) socket.close();
            }
            catch (Exception e) {}
        }

        // write a String to the Client output stream
        private boolean writeMsg(String msg) {
            // if Client is still connected send the message to it
            if(!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                sOutput.writeObject(msg);
            }
            // if an error occurs, do not abort just inform the user
            catch(IOException e) {
                display(notif + "Error sending message to " + username + notif);
                display(e.toString());
            }
            return true;
        }
    }
}
