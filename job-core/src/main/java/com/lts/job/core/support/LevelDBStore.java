package com.lts.job.core.support;

import com.lts.job.core.domain.KVPair;
import com.lts.job.core.file.FileAccessor;
import com.lts.job.core.file.FileException;
import com.lts.job.core.file.FileUtils;
import com.lts.job.core.util.JSONUtils;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * LevelDB 存储
 *
 * @author Robert HG (254963746@qq.com) on 3/6/15.
 */
public class LevelDBStore {

    // 文件锁 (同一时间只能有一个线程在 检查提交失败的任务)
    private FileAccessor dbLock;
    /**
     * 数据库目录
     */
    private File dbPath;

    private DB db;

    private Options options;

    public LevelDBStore(String path) {
        dbPath = FileUtils.createDirIfNotExist(path);
        options = new Options();
        try {
            dbLock = new FileAccessor(path + "___db.lock");
            dbLock.createIfNotExist();
        } catch (FileException e) {
            throw new RuntimeException(e);
        }
    }

    public void open() throws IOException {
        dbLock.tryLock();
        db = JniDBFactory.factory.open(dbPath, options);
    }

    public void put(String key, Object value) {
        String valueString = JSONUtils.toJSONString(value);
        db.put(key.getBytes(), valueString.getBytes());
    }

    public byte[] get(String key) {
        if (key == null) {
            return null;
        }
        return db.get(key.getBytes());
    }

    public <T> T get(String key, Type type) {
        if (key == null) {
            return null;
        }
        byte[] valueBytes = db.get(key.getBytes());
        return JSONUtils.parse(new String(valueBytes), type);
    }

    public void delete(String key) {
        if (key == null) {
            return;
        }
        db.delete(key.getBytes());
    }

    public void delete(List<String> keys) {
        if (keys == null || keys.size() == 0) {
            return;
        }
        for (String key : keys) {
            delete(key);
        }
    }

    public <T> List<KVPair<String, T>> getList(int size, Type type) {
        List<KVPair<String, T>> list = new ArrayList<KVPair<String, T>>(size);
        DBIterator iterator = db.iterator();
        for (iterator.seekToLast(); iterator.hasPrev(); iterator.prev()) {
            String key = new String(iterator.peekPrev().getKey());
            T value = JSONUtils.parse(new String(iterator.peekPrev().getValue()), type);
            KVPair<String, T> pair = new KVPair<String, T>(key, value);
            list.add(pair);
            if (list.size() >= size) {
                break;
            }
        }
        return list;
    }

    public void close() throws IOException {
        db.close();
        dbLock.unlock();
    }

    public void destroy() throws IOException {
        JniDBFactory.factory.destroy(dbPath, options);
        dbLock.delete();
    }

}
