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
        if (new File(fileName).delete()){
            channel.write(ByteBuffer.wrap("y".getBytes()));
        } else {
            channel.write(ByteBuffer.wrap("n".getBytes()));
        }
    }

    private static void rename(SocketChannel channel, String command) throws IOException{
        String[] splitCommand = command.split("\\?");
        File oldName = new File("serverfiles/"+splitCommand[0]);
        File newName = new File("serverfiles/"+splitCommand[1]);
        if (oldName.renameTo(newName)){
            channel.write(ByteBuffer.wrap("y".getBytes()));
        } else {
            channel.write(ByteBuffer.wrap("n".getBytes()));
        }
    }

    private static void senReplyCode(SocketChannel channel, char code) throws IOException {
        byte[] a = new byte[1];
        a[0] = (byte)code;
        ByteBuffer data = ByteBuffer.wrap(a);
        channel.write(data);
    }
}
