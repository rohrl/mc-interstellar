package io.github.rohrl.interstellar.client;

/** Implemented by each client world's chunk manager; never shares server objects. */
public interface WormholeChunkCache {
    int interstellar$remoteChunkCount();
    void interstellar$refreshRegions();
}
