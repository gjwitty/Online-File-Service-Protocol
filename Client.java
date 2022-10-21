import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;

public class Client {
    public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException{
        InetAddress host = InetAddress.getLocalHost();
        Socket socket = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        for(int i=0; i<5;i++){
            socket = new Socket(host.getHostName(), 9876);
            oos = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("Sending request to Server");
            ois = new ObjectInputStream(socket.getInputStream());
            String message = (String) ois.readObject();
            ois.close();
            oos.close();
        }
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
