package com.bcom.nsplacer.service;

import com.bcom.nsplacer.dao.FileDataDao;
import com.bcom.nsplacer.dao.FileEntryDao;
import com.bcom.nsplacer.model.FileData;
import com.bcom.nsplacer.model.FileEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author masoud
 */
@Service
public class FileEntryService extends BaseService<FileEntry> {

    private final FileEntryDao fileEntryDao;

    @Autowired
    private FileDataDao fileDataDao;

    @Autowired
    public <E extends Object> FileEntryService(FileEntryDao dao) {
        super(dao);
        this.fileEntryDao = dao;
    }

    public FileDataInputStream getInputStream(UUID id) {
        return new FileDataInputStream(fileEntryDao.findById(id).get().getFileDataId());
    }

    public FileDataOutputStream getOutputStream(UUID id) {
        return new FileDataOutputStream(fileEntryDao.findById(id).get().getFileDataId());
    }

    @Override
    public FileEntry create(FileEntry file) {
        FileData fileData = new FileData();
        fileData.setData(new byte[]{});
        fileData = fileDataDao.save(fileData);
        file.setFileDataId(fileData.getId());
        file = fileEntryDao.save(file);
        return file;
    }

    private UUID getUUIDFromArray(String split[], int offset) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            int b = Integer.parseInt(split[i + offset]) & 0xff;
            String h = String.format("%h", b);
            h = (h.length() == 1) ? ("0" + h) : h;
            sb.append(h);
            if (i == 3 || i == 5 || i == 7 || i == 9) {
                sb.append("-");
            }
        }
        return UUID.fromString(sb.toString());
    }

    private FileData fetchFileData(UUID id) {
        String result = fileDataDao.fetchIdAndNext(id);
        if (result != null) {
            FileData data = new FileData();
            String[] split = result.split(",");
            data.setId(getUUIDFromArray(split, 0));
            if (split.length == 32) {
                data.setNext(getUUIDFromArray(split, 16));
            }
            return data;
        }
        return null;
    }

    @Override
    public void delete(UUID id) {
        Optional<FileEntry> fileEntryOptional = fileEntryDao.findById(id);
        if (fileEntryOptional.isPresent()) {
            FileData data = fetchFileData(fileEntryOptional.get().getFileDataId());
            while (data != null) {
                UUID dataId = data.getId();
                UUID nextId = data.getNext();
                data = (nextId == null) ? null : fetchFileData(nextId);
                fileDataDao.deleteWithoutLoading(dataId);
            }
            fileEntryDao.deleteById(id);
        }
    }

    public List<FileEntry> findByName(String name) {
        return fileEntryDao.findByName(name);
    }

    public class FileDataInputStream extends InputStream {

        private UUID id;
        private ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{});

        public FileDataInputStream(UUID id) {
            this.id = id;
        }

        @Override
        public int read() throws IOException {
            byte b[] = new byte[1];
            int c = read(b, 0, b.length);
            return (c == 1) ? b[0] & 0xff : c;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (is.available() >= len) {
                return is.read(b, off, len);
            } else {
                int cnt = is.available();
                is.read(b, off, cnt);
                off += cnt;
                len -= cnt;
                while (true) {
                    Optional<FileData> data = (id == null) ? Optional.empty() : fileDataDao.findById(id);
                    if (data.isPresent()) {
                        id = data.get().getNext();
                        byte[] dataBytes = data.get().getData();
                        if (len <= dataBytes.length) {
                            System.arraycopy(dataBytes, 0, b, off, len);
                            is = new ByteArrayInputStream(dataBytes, len, dataBytes.length - len);
                            return cnt + len;
                        } else {
                            System.arraycopy(dataBytes, 0, b, off, dataBytes.length);
                            return cnt + dataBytes.length;
                        }
                    } else {
                        return cnt == 0 ? -1 : cnt;
                    }
                }
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }
    }

    public class FileDataOutputStream extends OutputStream {

        private UUID id;
        private ByteArrayOutputStream os = new ByteArrayOutputStream();
        private long length = 0;
        private boolean closed = false;

        public FileDataOutputStream(UUID id) {
            this.id = id;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }


        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (closed) {
                return;
            }
            os.write(b, 0, len);
            length += len;
            flushHelper(false);
        }

        @Override
        public void flush() throws IOException {
            flushHelper(false);
        }

        @Override
        public void close() throws IOException {
            flushHelper(true);
            closed = true;
        }

        private void flushHelper(boolean finish) {
            if (closed) {
                return;
            }
            if (finish || os.size() >= FileData.MAX_SIZE) {
                byte[] bytes = os.toByteArray();
                os.reset();
                for (int i = 0; i < bytes.length / FileData.MAX_SIZE; i++) {
                    byte buf[] = new byte[FileData.MAX_SIZE];
                    System.arraycopy(bytes, i * FileData.MAX_SIZE, buf, 0, buf.length);
                    FileData data = new FileData();
                    data.setId(id);
                    data.setData(buf);
                    FileData nextData = new FileData();
                    fileDataDao.save(nextData);
                    id = nextData.getId();
                    data.setNext(id);
                    fileDataDao.save(data);
                }
                if (bytes.length % FileData.MAX_SIZE != 0) {
                    if (finish) {
                        byte buf[] = new byte[bytes.length % FileData.MAX_SIZE];
                        System.arraycopy(bytes, (bytes.length / FileData.MAX_SIZE) * FileData.MAX_SIZE, buf, 0, buf.length);
                        FileData data = new FileData();
                        data.setId(id);
                        data.setData(buf);
                        fileDataDao.save(data);
                    } else {
                        os.write(bytes, (bytes.length / FileData.MAX_SIZE) * FileData.MAX_SIZE, bytes.length % FileData.MAX_SIZE);
                    }
                }
            }
        }

        public Long length() {
            return length;
        }
    }
}
