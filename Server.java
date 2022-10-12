import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;

public class Server {
    public static void main(String[] args){
        if (args.length != 1) {
            System.out.println("USAGE: Server <port>");
            return;
        }
        ServerSocketChannel listenChannel = ServerSocketChannel.open();
        listenChannel.bind(new InetSocketAddress(Integer.parseInt(args[0])));

        while (true) {
            SocketChannel serveChannel = listenChannel.accept();
            ByteBuffer commandBuffer = ByteBuffer.allocate(50);
            serveChannel.read(commandBuffer);
            buffer.flip();
            String command = commandBuffer.toString();
        }
    }
}
