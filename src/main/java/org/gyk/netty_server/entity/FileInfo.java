package org.gyk.netty_server.entity;

import lombok.Data;

import java.util.List;

@Data
public class FileInfo {

    private List<FileData> data;

    private String filename;

    private String contentType;


    private long size;

}
