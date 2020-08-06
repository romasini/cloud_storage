import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ClientHandlerNetty extends ChannelInboundHandlerAdapter {

    private String rootFolder;
    private AuthServer authServer;
    private int lengthInt, lengthPass;
    private String login;
    private String message;
    private Command currentCommand = Command.NO_COMMAND;
    private JobStage currentStage = JobStage.STANDBY;
    private String currentFilename;

    public ClientHandlerNetty(String rootFolder, AuthServer authServer) {
        this.rootFolder = rootFolder;
        this.authServer = authServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            //определение входящей команды
            if (currentStage == JobStage.STANDBY){
                currentCommand = CommandUtility.getCommand(buf.readByte());

                switch (currentCommand){
                    case AUTHORIZATION:
                        currentStage = JobStage.GET_LENGTH_LOGIN;
                        break;
//                    case GET_FILE_LIST:
//                        currentStage = JobStage.GET_LENGTH_LOGIN;
//                        break;
//                    case DOWNLOAD_FILE:
//                        currentStage = JobStage.GET_LENGTH_LOGIN;
//                        break;
                }


            }

            //последовательность действий
            if(currentCommand == Command.AUTHORIZATION){

                if(currentStage == JobStage.GET_LENGTH_LOGIN){
                    if (buf.readableBytes() >= 4) {
                        lengthInt = buf.readInt();
                        currentStage = JobStage.GET_LOGIN;
                    }
                }

                if(currentStage == JobStage.GET_LOGIN){
                    byte[] loginByte = new byte[lengthInt];
                    buf.readBytes(loginByte);
                    login = new String(loginByte, "UTF-8");
                    currentStage = JobStage.GET_LENGTH_PASS;
                }

                if(currentStage == JobStage.GET_LENGTH_PASS){
                    lengthPass = buf.readInt();
                    currentStage = JobStage.GET_PASS;
                }

                if(currentStage == JobStage.GET_PASS){
                    byte[] passByte = new byte[lengthPass];
                    buf.readBytes(passByte);
                    String pass = new String(passByte, "UTF-8");

                    ByteBuf answer = null;
                    if(authServer.authSuccess(login, pass)){
                        answer = ByteBufAllocator.DEFAULT.directBuffer(1);
                        answer.writeByte(Command.SUCCESS_AUTH.getCommandCode());
                        ctx.writeAndFlush(answer);
                    }else{
                        message = "Ошибка авторизации";

                        answer = ByteBufAllocator.DEFAULT.directBuffer(1);
                        answer.writeByte(Command.ERROR_SERVER.getCommandCode());
                        ctx.write(answer);

                        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                        answer = ByteBufAllocator.DEFAULT.directBuffer(4);
                        answer.writeInt(messageBytes.length);
                        ctx.write(answer);

                        answer = ByteBufAllocator.DEFAULT.directBuffer(messageBytes.length);
                        answer.writeBytes(messageBytes);
                        ctx.writeAndFlush(answer);
                    }
                    currentStage = JobStage.STANDBY;
                    currentCommand = Command.NO_COMMAND;
                }

            }

        }

        if (buf.readableBytes() == 0) {
            buf.release();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }


}
