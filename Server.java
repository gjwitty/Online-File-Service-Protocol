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
        ServerSocketChannel listenChannel = ServerSocketChannel.open();
        listenChannel.bind(new InetSocketAddress(Integer.parseInt(args[0])));
        String command;
        while (true) {
            SocketChannel serveChannel = listenChannel.accept();
            ByteBuffer commandBuffer = ByteBuffer.allocate(50);
            serveChannel.read(commandBuffer);
            commandBuffer.flip();
            command = commandBuffer.toString();
            while (command.charAt(0) != 'q') {
                try { 
                    switch (command.charAt(0)) {
                        case 'u': upload(serveChannel, command.substring(1)); break;
                        case 'd': download(serveChannel, command.substring(1)); break;
                        case 'r': remove(serveChannel, command.substring(1)); break;
                        case 'n': rename(serveChannel, command.substring(1)); break;
                        case 'l': list(serveChannel); break;
                        default: throw new IllegalArgumentException("ERROR: Operation "+command.charAt(0)+" not supported");
                    } 
                } catch (IOException e) { 
                    serveChannel.write(ByteBuffer.wrap(("ERROR: "+e.getMessage()).getBytes()));
                }
            } serveChannel.close();
        }
    }
    private static void upload(SocketChannel channel, String filename) throws IOException {
        if (!(new File("serverfiles/"+filename).createNewFile()))
            channel.write(ByteBuffer.wrap("n".getBytes()));
        else {
            channel.write(ByteBuffer.wrap("y".getBytes()));
            FileOutputStream stream = new FileOutputStream("serverfiles/"+filename);
            ByteBuffer buffer = ByteBuffer.allocate(1000000);
                // maximum filesize allowed is 1 MB
            channel.read(buffer);
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            try { stream.write(bytes); }
            catch (IOException e) {
                channel.write(ByteBuffer.wrap("n".getBytes()));
            } channel.write(ByteBuffer.wrap("y".getBytes()));
            stream.close();
        }
    }

    private static void download(SocketChannel channel, String filename) throws IOException {
        File file = new File("serverfiles/"+filename);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        ByteBuffer buffer = ByteBuffer.wrap(fileBytes);
        channel.write(buffer);
        buffer.clear();
        channel.read(buffer);
        buffer.flip();
        while ((char) buffer.get() != 'y') {
            buffer.clear();
            channel.write(ByteBuffer.wrap(fileBytes));
            channel.read(buffer);
            buffer.flip();
        }
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
