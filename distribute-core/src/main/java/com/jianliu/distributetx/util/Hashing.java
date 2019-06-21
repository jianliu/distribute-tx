package com.jianliu.distributetx.util;

import java.security.MessageDigest;

public interface Hashing {
    Hashing MURMUR_HASH = new MurmurHash();
    ThreadLocal<MessageDigest> md5Holder = new ThreadLocal<MessageDigest>();

    long hash(String key);

    long hash(byte[] key);
}