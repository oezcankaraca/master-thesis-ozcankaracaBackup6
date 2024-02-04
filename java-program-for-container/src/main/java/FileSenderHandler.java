import io.netty.channel.*;
import java.io.File;

/**
 * The FileSenderHandler class is a specialized Netty channel handler used for sending files over a network.
 * Extending the SimpleChannelInboundHandler class, it is designed to efficiently handle the process of reading
 * a file from the local file system and sending it over a network channel. This class is integral in scenarios
 * where file distribution or data dissemination is required in a distributed network system.
 *
 * This handler can be associated with either a SuperPeer or a LectureStudioServer, allowing it to be versatile
 * in different network roles. Upon activation of the channel, the handler reads the specified file and streams it
 * over the network to the connected peer. It keeps track of the start time to calculate the duration of the file
 * transfer, providing important metrics for network performance analysis.
 *
 * Furthermore, it implements a mechanism to send a confirmation message to a tracker-peer,
 * signifying the successful completion of the file transfer. This capability is crucial for maintaining
 * synchronization and coordination in distributed network operations, ensuring reliable and efficient
 * data exchange.
 *
 * @author Özcan Karaca
 */

public class FileSenderHandler extends SimpleChannelInboundHandler<Object> {
    private final String fileToSendPath;
    private long startTime;
    private long totalsentBytes = 0;

    private SuperPeer superPeer = null;
    private LectureStudioServer lectureStudioServer = null;

    /**
     * Constructor to initialize the FileSenderHandler with a file path and a
     * SuperPeer instance.
     * 
     * @param fileToSendPath  The path of the file to be sent.
     * @param superPeer The SuperPeer instance associated with this handler.
     */
    public FileSenderHandler(String fileToSendPath, SuperPeer superPeer) {
        this.fileToSendPath = fileToSendPath;
        this.superPeer = superPeer;
    }

    /**
     * Constructor to initialize the FileSenderHandler with a file path and a
     * LectureStudioServer instance.
     * 
     * @param fileToSendPath            The path of the file to be sent.
     * @param lectureStudioServer The LectureStudioServer instance associated with this handler.
     */
    public FileSenderHandler(String fileToSendPath, LectureStudioServer lectureStudioServer) {
        this.fileToSendPath = fileToSendPath;
        this.lectureStudioServer = lectureStudioServer;
    }

    /**
     * Activates the channel and starts the file sending process.
     * This method is triggered when the channel becomes active and is responsible for initiating
     * the file transfer. It opens the specified file and begins streaming its contents over the network.
     *
     * @param ctx The ChannelHandlerContext which provides access to the Channel,
     *            the EventLoop, and the ChannelPipeline.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        startTime = System.currentTimeMillis(); // Record the start time for measuring file transfer duration

        File file = new File(fileToSendPath); // Create a File object for the specified file path

        // Check if the file exists and is accessible
        if (file.exists()) {
            // Stream the file content to the channel. The DefaultFileRegion is used for
            // zero-copy transfer of the file content.
            ctx.writeAndFlush(new DefaultFileRegion(file, 0, file.length())).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    long transferDuration = System.currentTimeMillis() - startTime; // Calculating the file transfer duration

                    // Handling successful file transfer
                    if (future.isSuccess()) {

                        totalsentBytes = file.length();
                        System.out.println("Success: File sent successfully: " + fileToSendPath);
                        System.out.println("Info: Total sent bytes: " + totalsentBytes);
                        // Updating the file transfer duration in the associated SuperPeer or LectureStudioServer
                        if (superPeer != null) {
                            synchronized (superPeer) {
                                superPeer.setFileTransferDuration(transferDuration);
                            }
                        } else if (lectureStudioServer != null) {
                            synchronized (lectureStudioServer) {
                                lectureStudioServer.setFileTransferDuration(transferDuration);
                            }
                        }
                    } else {
                        // Handling errors in file transfer
                        System.err.println("Error: Error sending file: " + future.cause());
                    }

                    ctx.close(); // Closing the channel after the operation is complete
                }
            });
        } else {
            System.err.println("Error: File not found: " + fileToSendPath);
            ctx.close();
        }
    }

    /**
     * This method is overridden from SimpleChannelInboundHandler and is called when
     * a message is received from the channel.
     * However, it is not needed in the current context of FileSenderHandler as file
     * sending does not require handling incoming messages.
     *
     * @param ctx The ChannelHandlerContext which provides access to the Channel,
     *            the EventLoop, and the ChannelPipeline.
     * @param msg The received message.
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        // This method is not utilized in the context of sending files.
    }

    /**
     * Handles any exceptions thrown during channel operations.
     * Closes the channel in case of an error to prevent resource leaks and logs the error message.
     *
     * @param ctx   The ChannelHandlerContext which provides access to the Channel,
     *              the EventLoop, and the ChannelPipeline.
     * @param cause The Throwable that caused the exception.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("Error: An error occurred: " + cause.getMessage()); // Log the error message
        cause.printStackTrace(); // Print the stack trace for debugging
        ctx.close(); // Close the channel to prevent resource leaks
    }
}