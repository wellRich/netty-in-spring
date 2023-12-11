package org.gyk.netty_server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.gyk.netty_server.util.BodyReader;
import org.gyk.netty_server.util.JSONHelper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Matcher;

@Slf4j
@ChannelHandler.Sharable
@Component
public class SecondHandler extends ChannelInboundHandlerAdapter {

    @Resource
    private JSONHelper jsonHelper;

    public static final AttributeKey<BodyReader> KEY = AttributeKey.valueOf("IO");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        BodyReader bodyReader = (BodyReader) msg;
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer("success".getBytes()));
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    public void readBody(ByteBuf data, BodyReader bodyReader) {
        DataBuffer readBuffer = bodyReader.getReadBuffer();
        int needToReadBytes = bodyReader.getBytes();
        if (needToReadBytes > 0) {
            int readableByteCount = data.readableBytes();
            //可读字节数小于需要读取的字节数
            if (needToReadBytes > readableByteCount) {
                byte[] c = bodyReader.getCachedBytes();
                data.readBytes(c, c.length - needToReadBytes, readableByteCount);
                bodyReader.setBytes(needToReadBytes - readableByteCount);
            } else {
                byte[] c = bodyReader.getCachedBytes();
                data.readBytes(c, c.length - needToReadBytes, needToReadBytes);
                bodyReader.setBytes(0);
                bodyReader.setCachedBytes(null);
                bodyReader.setSkip(bodyReader.getBoundaryPrefixLength() + 2);
                readBody(data, bodyReader);
            }
        } else {
            //skip逻辑
            long skip = bodyReader.getSkip();
            if (skip > 0) {
                int readableByteCount = data.readableBytes();
                if (skip > readableByteCount) {
                    bodyReader.setSkip(skip - readableByteCount);
                } else {
                    int readPosition = data.readerIndex() + (int) skip;
                    data.readerIndex(readPosition);
                    bodyReader.setSkip(0);
                    if (data.readerIndex() == 4) {
                        byte[] tmp = new byte[data.readerIndex()];
                        data.readBytes(tmp);
                        if (bodyReader.isEnd(tmp)) {
                            bodyReader.clean();
                            return;
                        } else {
                            data.readerIndex(readPosition);
                        }
                    }
                }
            }
            if (bodyReader.getSkip() == 0) {
                byte b;
                while (data.readableBytes() > 0 && bodyReader.getCrflCount() < BodyReader.MATCH_FINISHED_FLAG) {
                    b = data.readByte();
                    if (Objects.equals(b, bodyReader.getExpectedByte())) {
                        bodyReader.cleanExpectedByte();
                        bodyReader.incrementNumOfCRLF();
                        if (bodyReader.matched()) {
                            break;
                        }
                    } else if (b == BodyReader.CR) {
                        //到了头部的结尾
                        bodyReader.setExpectedByte(BodyReader.LF);
                    } else {
                        readBuffer.write(b);
                    }
                }
                //解析头部信息
                if (bodyReader.matched()) {
                    bodyReader.clean();
                    byte[] h = new byte[readBuffer.readableByteCount()];
                    readBuffer.read(h);
                    String info = new String(h, StandardCharsets.UTF_8);
                    Matcher matcher = BodyReader.FILENAME_MATCH.matcher(info);
                    if (matcher.matches()) {
                        //文件头信息
                        int fileSize = Integer.parseInt(matcher.group(2));
                        bodyReader.setCacheFileFormName(matcher.group(1));
                        byte[] fileData = new byte[fileSize];
                        bodyReader.setCachedBytes(fileData);
                        bodyReader.addFile(bodyReader.getCacheFileFormName(), fileData);
                        //长度足够就读，否则设置状态
                        log.info("file------info={},  fileSize--->{}", info, fileSize);
                        bodyReader.incrementNumOfCRLF();
                        readBody(data, bodyReader);
                    } else {
                        Matcher fieldNameMatcher = BodyReader.FIELD_NAME_MATCH.matcher(info);
                        if (fieldNameMatcher.matches()) {
                            log.info("plain-text-head---info={}", info);
                            //文本字段头信息，接下来读文本内容，no skip
                            bodyReader.setCachedTextFieldName(fieldNameMatcher.group(1));
                            if (data.readableBytes() > 0) {
                                readBody(data, bodyReader);
                            }
                        } else {
                            Matcher contentTypeMatcher = BodyReader.CONTENT_TYPE_MATCH.matcher(info);
                            if (contentTypeMatcher.matches()) {
                                log.info("content-type---info={}", info);
                                //解析到文件第二个头部信息——content-type
                                long step = 2 + bodyReader.getCachedBytes().length;
                                if (data.readableBytes() >= step) {
                                    data.readerIndex(data.readerIndex() + 2);
                                    data.readBytes(bodyReader.getCachedBytes());
                                    bodyReader.setBytes(0);
                                    b = data.readByte();
                                    log.info("b-->{}", b);
                                    if (b != BodyReader.DASH) {
                                        bodyReader.setExpectedByte(BodyReader.LF);
                                        readBody(data, bodyReader);
                                    }
                                } else {
                                    int part = data.readableBytes();
                                    if (part > 2) {
                                        data.readerIndex(data.readerIndex() + 2);
                                        int len = part - 2;
                                        data.readBytes(bodyReader.getCachedBytes(), 0, len);
                                        bodyReader.setBytes(bodyReader.getCachedBytes().length - len);
                                    } else {
                                        bodyReader.setBytes(bodyReader.getCachedBytes().length);
                                    }
                                }
                            } else {
                                //文本内容
                                bodyReader.getFormFields().put(bodyReader.getCachedTextFieldName(), info);
                                long step = bodyReader.getBoundaryPrefixLength();
                                if (StringUtils.isNotBlank(info)) {
                                    if (info.length() > 200) {
                                        log.info("text---info={}", info.substring(0, 200).contains("..."));
                                    } else {
                                        log.info("text---info={}", info);
                                    }
                                }
                                //每读完一个字段要看看是否要结束了
                                if (data.readableBytes() > step) {
                                    data.readerIndex((int) step + data.readerIndex());
                                    b = data.readByte();
                                    if (b != BodyReader.DASH) {
                                        bodyReader.setExpectedByte(BodyReader.LF);
                                        readBody(data, bodyReader);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
