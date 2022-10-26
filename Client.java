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
            System.out.println("Usage: java Client <server_IP> <server_port>");
            return;
        }
        int serverPort = Integer.parseInt(args[1]);
        String serverIP = args[0];

        char command;

        do {
            Scanner keyboard = new Scanner(System.in);
            System.out.println("Enter a command (D, G, L, R):");
            //Commands are NOT case-sensitive.
            command = keyboard.nextLine().toUpperCase().charAt(0);

            switch (command) {
                case 'G':
                    System.out.println("Enter the name of the file to download: ");
                    String fileName = keyboard.nextLine();
                    ByteBuffer buffer = ByteBuffer.wrap(("G" + fileName).getBytes());
                    SocketChannel channel = SocketChannel.open();
                    channel.connect(new InetSocketAddress(serverIP, serverPort));
                    channel.write(buffer);
                    //It's critical to shut down output on client side
                    //when client is done sending to server
                    channel.shutdownOutput();
                    //receive server reply code
                    if (getServerCode(channel) != 'S') {
                        System.out.println("Server failed to serve the request.");
                    } else {
                        System.out.println("The request was accepted");
                        Files.createDirectories(Paths.get("./downloaded"));
                        //make sure to set the "append" flag to true
                        BufferedWriter bw = new BufferedWriter(new FileWriter("./downloaded/" + fileName, true));
                        ByteBuffer data = ByteBuffer.allocate(1024);
                        int bytesRead;

                        while ((bytesRead = channel.read(data)) != -1) {
                            //before reading from buffer, flip buffer
                            //("limit" set to current position, "position" set to zero)
                            data.flip();
                            byte[] a = new byte[bytesRead];
                            //copy bytes from buffer to array
                            //(all bytes between "position" and "limit" are copied)
                            data.get(a);
                            String serverMessage = new String(a);
                            bw.write(serverMessage);
                            data.clear();
                        }
                        bw.close();
                    }
                    channel.close();
                    break;
            }
        } while (command != 'Q');

    }

    private static char getServerCode(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        int bytesToRead = 1;

        //make sure we read the entire server reply
        while ((bytesToRead -= channel.read(buffer)) > 0) ;

        //before reading from buffer, flip buffer
        buffer.flip();
        byte[] a = new byte[1];
        //copy bytes from buffer to array
        buffer.get(a);
        char serverReplyCode = new String(a).charAt(0);

        //System.out.println(serverReplyCode);

        return serverReplyCode;
    }

    private static void upload(SocketChannel channel, String fileName) throws IOException {
        File file = new File(fileName);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        ByteBuffer buffer = ByteBuffer.wrap(fileBytes);
        channel.write(buffer);
        buffer.clear();
        channel.read(buffer);
        buffer.flip();
        while ((char) buffer.get() != 'y'){
            buffer.clear();
            channel.write(ByteBuffer.wrap(fileBytes));
            channel.read(buffer);
            buffer.flip();
        }
    }

    private static void download(SocketChannel channel, String fileName) throws IOException{
        if (!(new File(fileName).createNewFile())){
            channel.write(ByteBuffer.wrap("y".getBytes()));
        } else {
            FileOutputStream stream = new FileOutputStream(fileName);
            ByteBuffer buffer = ByteBuffer.allocate(1000000);
            channel.read(buffer);
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            try{ stream.write(bytes);}
            catch (IOException e){
                channel.write(ByteBuffer.wrap("n".getBytes()));
            } channel.write(ByteBuffer.wrap("y".getBytes()));
            stream.close();
        }
    }

    private static void remove(SocketChannel channel, String fileName) throws IOException{
        channel.write(ByteBuffer.wrap(("r"+fileName).getBytes()));
        ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        channel.read(byteBuffer);
        if ((char) byteBuffer.get() == 'y'){
            System.out.println("File removed");
        } else {
            System.out.println("Error");
        }
    }

    private static void rename(SocketChannel channel, String command) throws IOException{
        channel.write(ByteBuffer.wrap(("n"+command).getBytes()));
        ByteBuffer byteBuffer = ByteBuffer.allocate(1);
        if ((char) byteBuffer.get() == 'y'){
            System.out.println("Files renamed");
        } else {
            System.out.println("Error");
        }
    }

    private static void list(SocketChannel channel) throws IOException {
        channel.write(ByteBuffer.wrap(("l").getBytes()));

    }

    private static void sendReplyCode(SocketChannel channel, char code) throws IOException {
        byte[] a = new byte[1];
        a[0] = (byte)code;
        ByteBuffer data = ByteBuffer.wrap(a);
        channel.write(data);
    }
}
