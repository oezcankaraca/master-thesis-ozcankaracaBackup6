import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * The FileReceiverHandler class is a Netty channel handler specifically designed for receiving files over a network.
 * Extending the SimpleChannelInboundHandler class, it efficiently handles the data received from a network channel,
 * writing it to a specified file path. This class plays a crucial role in network-based file transfer operations,
 * making it a key component in distributed systems where file sharing or data exchange is required.
 *
 * This handler supports functionality for both the Peer and SuperPeer classes,
 * making it versatile for various network roles. Upon activation of the channel,
 * it initializes the file output stream to start receiving the data.
 * As the data arrives, it writes the bytes to the file and keeps track of the total number of bytes received.
 * Upon completion of the file transfer, it calculates the transfer duration,
 * logs the details, and notifies the associated Peer or SuperPeer instance.
 *
 * Additionally, it features a method to send a confirmation message to a tracker-peer after
 * successful file reception, enhancing its utility in network management and coordination scenarios.
 *
 * @author Özcan Karaca
 */

public class FileReceiverHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final String fileToReceivePath;
    private FileOutputStream fileOutputStream;
    private long totalReceivedBytes = 0;
    private long startTime;

    private final Peer peer;
    private final SuperPeer superPeer;

    /**
     * Constructor for creating a FileReceiverHandler with a file path and a Peer instance.
     * 
     * @param fileToReceivePath The path where the received file will be saved.
     * @param peer     The Peer instance associated with this handler.
     */
    public FileReceiverHandler(String fileToReceivePath, Peer peer) {
        this.fileToReceivePath = fileToReceivePath;
        this.peer = peer;
        this.superPeer = null; // SuperPeer is not used in this context
    }

    /**
     * Constructor for creating a FileReceiverHandler with a file path and a SuperPeer instance.
     * 
     * @param fileToReceivePath  The path where the received file will be saved.
     * @param superPeer The SuperPeer instance associated with this handler.
     */
    public FileReceiverHandler(String fileToReceivePath, SuperPeer superPeer) {
        this.fileToReceivePath = fileToReceivePath;
        this.superPeer = superPeer;
        this.peer = null; // Peer is not used in this context
    }

    /**
     * Handles the channel activation event. Initializes file output stream for writing received bytes to the file.
     * 
     * @param ctx The ChannelHandlerContext which provides access to the Channel,
     *            the EventLoop, and the ChannelPipeline.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        startTime = System.currentTimeMillis();
        try {
            File file = new File(fileToReceivePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            fileOutputStream = new FileOutputStream(file);
        } catch (IOException e) {
            System.err.println("Error: Error while opening file output stream: " + e.getMessage());
            ctx.close();
        }
    }

    /**
     * Handles the incoming data (bytes) from the channel. Writes the received bytes to the file output stream.
     * 
     * @param ctx The ChannelHandlerContext which provides access to the Channel,
     *            the EventLoop, and the ChannelPipeline.
     * @param msg The received ByteBuf containing the data.
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        fileOutputStream.write(bytes);
        totalReceivedBytes += bytes.length; // Update the total received bytes count
    }

    /**
     * Handles the channel inactivity event, which indicates the end of file transfer.
     * It calculates the file transfer duration, logs the transfer details, and
     * notifies the associated peer or super peer.
     * 
     * @param ctx The ChannelHandlerContext which provides access to the Channel,
     *            the EventLoop, and the ChannelPipeline.
     * @throws Exception If an error occurs during file closing or channel closing operations.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        long transferDuration = System.currentTimeMillis() - startTime;

        // Log file transfer details
        System.out.println("Success: File received successfully: " + fileToReceivePath);
        System.out.println("Info: Total received bytes: " + totalReceivedBytes);
        System.out.println("Info: File Transfer Time: " + transferDuration + " ms");

        // Set file transfer duration in the associated peer or super peer and notify it
        if (peer != null) {
            peer.setFileTransferDuration(transferDuration);
            synchronized (peer) {
                peer.notify();
            }
        } else if (superPeer != null) {
            superPeer.setFileTransferDuration(transferDuration);
            superPeer.setFileReceived(true);
            synchronized (superPeer) {
                superPeer.notify();
            }
        }


        fileOutputStream.close(); // Close the file output stream
        ctx.close(); // Close the network channel
    }


    /**
     * Handles exceptions caught during the channel operations.
     * Logs the error message and stack trace for debugging purposes, and closes the
     * channel to prevent resource leaks.
     * 
     * @param ctx   The ChannelHandlerContext which provides access to the Channel,
     *              the EventLoop, and the ChannelPipeline.
     * @param cause The Throwable that caused the exception.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("Error: An error occurred: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}