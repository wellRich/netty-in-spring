package org.gyk.netty_server.util;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * multipart-form-data类型表单解析
 * 解析文本字段与文件长度
 * 目的是比较自定义解析与tomcat解析之间的速度
 */
@Slf4j
public final class BodyReader implements Serializable {


    public final int boundaryPrefixLength;

    //解析出来的信息
    private DataBuffer readBuffer;

    //文件字节总数
    private long fileByteCount = 0;

    //表单字段中的非文件字段
    private final Map<String, String> formFields = new HashMap<>();

    private final Map<String, List<byte[]>> uploadFiles = new HashMap<>();

    public boolean beginRead = false;

    //字符匹配数量
    public int matchCount = 0;

    //需要跳过的字节数
    private long skip = 0;

    //\r\n的计数
    public int crflCount = 0;

    public String cachedTextFieldName;

    /**
     * 需要读取的字节
     */
    private int bytes = 0;

    private byte[] cachedBytes;

    private String cacheFileFormName;

    //期望的字节
    private Byte expectedByte;

    //开始时间
    private long startTime;

    private Throwable throwable;

    public static final byte CR = 0x0D;

    /**
     * The Line Feed ASCII character value.
     */
    public static final byte LF = 0x0A;

    /**
     * The dash (-) ASCII character value.
     */
    public static final byte DASH = 0x2D;

    private static final byte[] END = {DASH, DASH, CR, LF};

    public static final String BOUNDARY_FLAG = "boundary=";


    /**
     * 文件名的匹配
     */
    public final static Pattern FILENAME_MATCH = Pattern.compile(".*\\sname=\"(.*)\";\\sfilename=\"(\\d+)_.*\".*");
    /**
     * 表单普通字段的头信息
     */
    public final static Pattern FIELD_NAME_MATCH = Pattern.compile("^Content-Disposition:\\sform-data;\\sname=\"(.*)\".*");

    /**
     * 文件类型头信息
     */
    public final static Pattern CONTENT_TYPE_MATCH = Pattern.compile("^Content-Type:\\s.*");

    public final static int MATCH_FINISHED_FLAG = 2;

    public BodyReader(int boundaryPrefixLength) {
        this.boundaryPrefixLength = boundaryPrefixLength;
        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
        this.readBuffer = dataBufferFactory.allocateBuffer();
    }

    private boolean arrayEquals(byte[] a) {
        for (int i = 0; i < 4; i++) {
            if (a[i] != BodyReader.END[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean isEnd(byte[] tmp) {
        return arrayEquals(tmp);
    }

    public boolean hasBeginParse(){
        return beginRead;
    }

    public void beginParse() {
        if (!this.beginRead) {
            this.beginRead = true;
            this.startTime = System.nanoTime();
        } else {
            throw new UnsupportedOperationException("解析的标志已设置，不可重复设置");
        }
    }

    public byte[] getCachedBytes() {
        return cachedBytes;
    }

    public void setCachedBytes(byte[] cachedBytes) {
        this.cachedBytes = cachedBytes;
    }

    public String getCacheFileFormName() {
        return cacheFileFormName;
    }

    public void setCacheFileFormName(String cacheFileFormName) {
        this.cacheFileFormName = cacheFileFormName;
    }

    public boolean matched() {
        return this.crflCount == MATCH_FINISHED_FLAG;
    }

    public void clean() {
        this.crflCount = 0;
        this.expectedByte = null;
        this.matchCount = 0;
        this.readBuffer = readBuffer.factory().allocateBuffer();
    }

    public void addFile(String fileName, byte[] data){
        uploadFiles.compute(fileName, (k, v) -> {
           if(v != null){
               v.add(data);
               return v;
           }else {
               List<byte[]> d = new ArrayList<>();
               d.add(data);
               return d;
           }
        });
    }


    public Map<String, List<byte[]>> getUploadFiles(){
        return uploadFiles;
    }

    public void incrementNumOfCRLF() {
        this.crflCount++;
    }

    public int getBytes() {
        return bytes;
    }

    public void setBytes(int fileSize) {
        this.bytes = fileSize;
    }

    public String getCachedTextFieldName() {
        return cachedTextFieldName;
    }

    public void setCachedTextFieldName(String cachedTextFieldName) {
        this.cachedTextFieldName = cachedTextFieldName;
    }

    public int getBoundaryPrefixLength() {
        return boundaryPrefixLength;
    }

    public Byte getExpectedByte() {
        return expectedByte;
    }

    public void cleanExpectedByte() {
        this.expectedByte = null;
    }

    public void setExpectedByte(Byte expectedByte) {
        this.expectedByte = expectedByte;
    }

    public int getCrflCount() {
        return crflCount;
    }

    public DataBuffer getReadBuffer() {
        return readBuffer;
    }

    public long getFileByteCount() {
        return fileByteCount;
    }

    public void setFileByteCount(long fileByteCount) {
        this.fileByteCount = fileByteCount;
    }

    public Map<String, String> getFormFields() {
        return formFields;
    }

    public long getSkip() {
        return skip;
    }

    public void setSkip(long skip) {
        this.skip = skip;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
