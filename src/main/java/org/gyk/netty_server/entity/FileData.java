package org.gyk.netty_server.entity;

import io.netty.buffer.ByteBuf;

//文件缓存
public class FileData {

    private int offset;

    private int end;

    private ByteBuf content;

    public FileData(int offset, int end, ByteBuf content) {
        this.offset = offset;
        this.end = end;
        this.content = content;
    }

    private FileData next;

    public FileData getNext() {
        return next;
    }

    public void setNext(FileData next) {
        this.next = next;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public ByteBuf getContent() {
        return content;
    }

    public void setContent(ByteBuf content) {
        this.content = content;
    }
}
