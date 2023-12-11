package org.gyk.netty_server.entity;

import io.netty.buffer.ByteBuf;

public class FileData {

    private int offset;

    private int end;

    private ByteBuf content;

    public FileData(int offset, int end, ByteBuf content) {
        this.offset = offset;
        this.end = end;
        this.content = content;
    }
}
