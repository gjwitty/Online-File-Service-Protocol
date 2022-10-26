import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;

public class Server {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("USAGE: Server <port>");
            return;
        }
        File serverfiles = new File("serverfiles");
        if (!serverfiles.isDirectory()) serverfiles.mkdirs();
        ServerSocketChannel listenChannel = ServerSocketChannel.open();
        listenChannel.bind(new InetSocketAddress(Integer.parseInt(args[0])));
        String command;
        while (true) {
            SocketChannel serveChannel = listenChannel.accept();
            ByteBuffer commandBuffer = ByteBuffer.allocate(129);
            while(serveChannel.read(commandBuffer) >= 0);
            // client shuts down the output
            commandBuffer.flip();
            byte[] asBytes = new byte[commandBuffer.remaining()];
            commandBuffer.get(asBytes);
            command = new String(asBytes);
                try { 
                    switch (command.charAt(0)) {
                        case 'd': download(serveChannel, command.substring(1)); break;
                        case 'r': remove(serveChannel, command.substring(1)); break;
                        case 'n': rename(serveChannel, command.substring(1)); break;
                        case 'l': list(serveChannel); break;
                        default: throw new IllegalArgumentException("ERROR: Operation "+command.charAt(0)+" not supported");
                    } 
                } catch (IOException e) { 
                    System.err.println("ERROR: "+e.getMessage());
                }
            serveChannel.close();
        }
    }

    private static void download(SocketChannel channel, String filename) throws IOException {
        File file = new File("serverfiles/"+filename);
        if (!file.exists()) channel.write(ByteBuffer.wrap("n".getBytes()));
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        ByteBuffer buffer = ByteBuffer.wrap(fileBytes);
        channel.write(buffer);
    }

    private static void remove(SocketChannel channel, String filename) throws IOException {
        if (new File("serverfiles/"+filename).delete())
            channel.write(ByteBuffer.wrap("y".getBytes()));
        else
            channel.write(ByteBuffer.wrap("n".getBytes()));
    }
    
    private static void rename(SocketChannel channel, String command) throws IOException {
        String[] splitCommand = command.split("\\?");
        File oldName = new File("serverfiles/"+splitCommand[0]);
        File newName = new File("serverfiles/"+splitCommand[1]);
        if (oldName.renameTo(newName))
            channel.write(ByteBuffer.wrap("y".getBytes()));
        else
            channel.write(ByteBuffer.wrap("n".getBytes()));
    }

    private static void list(SocketChannel channel) throws IOException {
        String[] files = new File("serverfiles").list();
        String filesString = String.join("\t", files);
        channel.write(ByteBuffer.wrap(filesString.getBytes()));
    }
}
