import callback.Callback;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    private Callback authCallBack, getFileListCallBack, downloadFileCallBack, uploadFileCallBack, errorCallBack;
    private Command currentCommand = Command.NO_COMMAND;
    private JobStage currentStage = JobStage.STANDBY;
    private int lengthInt;
    private String currentFolder, currentFilename;
    private long currentFileLength;
    private Path downloadFile;
    private RandomAccessFile aFile;
    private FileChannel inChannel;
    private long counter = 0;
    private int tempCount = 0;

    public ServerHandler(Callback authCallBack, Callback getFileListCallBack, Callback downloadFileCallBack, Callback uploadFileCallBack, Callback errorCallBack) {
        this.authCallBack = authCallBack;
        this.getFileListCallBack = getFileListCallBack;
        this.downloadFileCallBack = downloadFileCallBack;
        this.uploadFileCallBack = uploadFileCallBack;
        this.errorCallBack = errorCallBack;
        this.currentFolder = "clientFolder";
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            //определение входящей команды
            if (currentStage == JobStage.STANDBY) {
                currentCommand = CommandUtility.getCommand(buf.readByte());

                switch (currentCommand) {
                    case SUCCESS_AUTH:
                        if(authCallBack!=null)
                            authCallBack.call();
                        break;
                    case ERROR_SERVER:
                        currentStage =  JobStage.GET_ERROR_LENGTH;
                        break;
                    case RETURN_FILE_LIST:
                        currentStage = JobStage.GET_FILE_LIST_LENGTH;
                        break;
                    case DOWNLOAD_FILE_PROCESS:
                        currentStage = JobStage.GET_FILE_NAME_LENGTH;
                        break;
                    case DELETE_SUCCESS:
                        currentStage = JobStage.GET_FILE_NAME_LENGTH;
                        break;
                }
            }

            if(currentCommand == Command.ERROR_SERVER){
                if(currentStage == JobStage.GET_ERROR_LENGTH){
                    if (buf.readableBytes() >= 4) {
                        lengthInt = buf.readInt();
                        currentStage = JobStage.GET_ERROR_MESSAGE;
                    }
                }

                if(currentStage == JobStage.GET_ERROR_MESSAGE){
                    if (buf.readableBytes() >= lengthInt) {
                        byte[] arrayFilesByte = new byte[lengthInt];
                        buf.readBytes(arrayFilesByte);
                        String errorMessage = new String(arrayFilesByte, StandardCharsets.UTF_8);

                        if(errorCallBack!=null)
                            errorCallBack.call(errorMessage);

                        currentStage = JobStage.STANDBY;
                        currentCommand = Command.NO_COMMAND;
                    }
                }

            }

            if (currentCommand == Command.RETURN_FILE_LIST){

                if(currentStage == JobStage.GET_FILE_LIST_LENGTH){
                    if (buf.readableBytes() >= 4) {
                        lengthInt = buf.readInt();
                        currentStage = JobStage.GET_FILE_LIST;
                    }
                }

                if(currentStage == JobStage.GET_FILE_LIST){
                    if (buf.readableBytes() >= lengthInt) {
                        byte[] arrayFilesByte = new byte[lengthInt];
                        buf.readBytes(arrayFilesByte);
                        String[] arrayFiles = new String(arrayFilesByte, StandardCharsets.UTF_8).split("/");

                        List<String> listFiles = Arrays.asList(arrayFiles);
                        getFileListCallBack.call(listFiles);
                        currentStage = JobStage.STANDBY;
                        currentCommand = Command.NO_COMMAND;
                    }
                }
            }

            if(currentCommand == Command.DOWNLOAD_FILE_PROCESS){

                if(currentStage==JobStage.GET_FILE_NAME_LENGTH){
                    if (buf.readableBytes() >= 4) {
                        lengthInt = buf.readInt();
                        currentStage = JobStage.GET_FILE_NAME;
                    }
                }

                if(currentStage == JobStage.GET_FILE_NAME){
                    if (buf.readableBytes() >= lengthInt) {
                        byte[] fileNameByte = new byte[lengthInt];
                        buf.readBytes(fileNameByte);
                        currentFilename = new String(fileNameByte, "UTF-8");
                        currentStage = JobStage.GET_FILE_LENGTH;
                    }
                }

                if(currentStage == JobStage.GET_FILE_LENGTH){
                    if (buf.readableBytes() >= 8) {
                        currentFileLength = buf.readLong();
                        currentStage = JobStage.GET_FILE;

                        downloadFile = Paths.get(currentFolder, currentFilename);
                        aFile = new RandomAccessFile(downloadFile.toFile(), "rw");
                        inChannel = aFile.getChannel();
                        counter = 0;
                        tempCount = 0;
                    }
                }

                if(currentStage == JobStage.GET_FILE){

                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    byte[] fileBytes = null;
                    while (buf.readableBytes()>0 && counter < currentFileLength){
                        long tempPackLength = buf.readableBytes();

                        while(tempPackLength>0) {
                            if(tempPackLength>1024) {
                                fileBytes = new byte[1024];
                            }else{
                                fileBytes = new byte[(int)tempPackLength];
                            }

                            buf.readBytes(fileBytes);

                            byteBuffer.clear();
                            byteBuffer.put(fileBytes);
                            byteBuffer.flip();

                            while (byteBuffer.hasRemaining()) {
                                tempCount = inChannel.write(byteBuffer);
                                counter = counter + tempCount;
                            }

                            tempPackLength = tempPackLength - 1024;
                        }

                    }

                     if(counter == currentFileLength) {
                        aFile.close();
                        currentStage = JobStage.STANDBY;
                        currentCommand = Command.NO_COMMAND;
                    }

                }
            }

            if(currentCommand == Command.DELETE_SUCCESS){

                if(currentStage==JobStage.GET_FILE_NAME_LENGTH){
                    if (buf.readableBytes() >= 4) {
                        lengthInt = buf.readInt();
                        currentStage = JobStage.GET_FILE_NAME;
                    }
                }

                if(currentStage == JobStage.GET_FILE_NAME){
                    if (buf.readableBytes() >= lengthInt) {
                        byte[] fileNameByte = new byte[lengthInt];
                        buf.readBytes(fileNameByte);
                        currentFilename = new String(fileNameByte, "UTF-8");

                        System.out.println(currentFilename + " успешно удален");

                        currentStage = JobStage.STANDBY;
                        currentCommand = Command.NO_COMMAND;
                    }
                }

            }

            if (buf.readableBytes() == 0) {
                buf.release();
            }

        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
