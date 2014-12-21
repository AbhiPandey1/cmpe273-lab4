package edu.sjsu.cmpe.cache.client;


public interface CacheServiceInterface {
    public String get(long key);

    public void put(long key, String value);

    public void delete(long key);
}
