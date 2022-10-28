import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {
        if (args.length != 2) {
            System.out.println("USAGE: ClientTCP <IP> <port>");
            return;
        } InetAddress serverIP;
        try {
            serverIP = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            System.err.println("Unknown Host: "+args[0]);
            return;
        }
        int serverPort = Integer.parseInt(args[1]);
        SocketChannel clientChannel;
        // calling SocketChannel.open with a parameter calls connect() automatically!
        clientChannel = SocketChannel.open(new InetSocketAddress(serverIP, serverPort));
        String command;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter d, r, n, l");
        command = scanner.nextLine();
        String fileName = "";
        if (!command.equals("l")){
            System.out.println("Enter the File Name");
            fileName = scanner.nextLine();
        }
        try {
            switch (command.charAt(0)) {
                case 'd': download(clientChannel, fileName); break;
                case 'r': remove(clientChannel, fileName); break;
                case 'n':
                    System.out.println("Enter a new File Name:");
                    fileName += "?" + scanner.nextLine();
                    rename(clientChannel, fileName); break;
                case 'l': list(clientChannel); break;
                default: throw new IllegalArgumentException("ERROR: Operation "+command.charAt(0)+" not supported");
            }
        } catch (IOException e) {
            System.err.println("ERROR: "+e.getMessage());
        }
        clientChannel.close();
    }

    private static void download(SocketChannel channel, String fileName) throws IOException{
        channel.write(ByteBuffer.wrap(("d"+fileName).getBytes()));
        channel.shutdownOutput();
        if (!(new File(fileName).createNewFile())){
            System.out.println("Error");
        } else {
            FileOutputStream stream = new FileOutputStream(fileName);
            ByteBuffer buffer = ByteBuffer.allocate(1000000);
            channel.read(buffer);
            buffer.flip();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            stream.write(bytes);
            stream.close();
            System.out.println("File "+fileName+" downloaded!");
        }
    }

    private static void remove(SocketChannel channel, String fileName) throws IOException{
        channel.write(ByteBuffer.wrap(("r"+fileName).getBytes()));
        channel.shutdownOutput();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        channel.read(byteBuffer);
        byteBuffer.flip();
        if ((char) byteBuffer.get() == 'y'){
            System.out.println("File removed");
        } else {
            System.out.println("Error");
        }
    }

    private static void rename(SocketChannel channel, String command) throws IOException{
        channel.write(ByteBuffer.wrap(("n"+command).getBytes()));
        ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        channel.shutdownOutput();
        channel.read(byteBuffer);
        byteBuffer.flip();
        if ((char) byteBuffer.get() == 'y'){
            System.out.println("File "+command.split("\\?")[0]+" renamed to "+command.split("\\?")[1]);
        } else {
            System.out.println("Error");
        }
    }

    private static void list(SocketChannel channel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(10000);
        channel.write(ByteBuffer.wrap(("l").getBytes()));
        channel.shutdownOutput();
        while (channel.read(byteBuffer) >= 0);
        byteBuffer.flip();
        byte[] asBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(asBytes);
        System.out.println(new String(asBytes));
    }
}
