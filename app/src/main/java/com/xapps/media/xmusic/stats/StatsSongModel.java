package com.xapps.media.xmusic.stats;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public final class StatsSongModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private long mediaStoreId;
    private String title;
    private String artist;
    private String album;
    private String path;
    private long size;
    private long lastModified;
    private int playCount;
    private int lastVerifiedPlayCount;
    private String weakHash;
    private List<Long> fingerprints;

    public StatsSongModel(long mediaStoreId, String title, String artist, String album, String path, long size, List<Long> fingerprints) {
        this.mediaStoreId = mediaStoreId;
        this.title = title != null ? title : "";
        this.artist = artist != null ? artist : "";
        this.album = album != null ? album : "";
        this.path = path != null ? path : "";
        this.size = size;
        this.lastModified = new File(this.path).lastModified();
        this.fingerprints = fingerprints;
        this.playCount = 1;
        this.lastVerifiedPlayCount = 1;
        this.weakHash = calculateWeakHash();
    }

    public String calculateWeakHash() {
        String raw = mediaStoreId + "|" + title + "|" + artist + "|" + album + "|" + path + "|" + size + "|" + lastModified;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(raw.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }

    public long getMediaStoreId() { return mediaStoreId; }
    public void setMediaStoreId(long mediaStoreId) { this.mediaStoreId = mediaStoreId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public int getPlayCount() { return playCount; }
    public void incrementPlayCount() { this.playCount++; }

    public int getLastVerifiedPlayCount() { return lastVerifiedPlayCount; }
    public void updateLastVerifiedPlayCount() { this.lastVerifiedPlayCount = this.playCount; }

    public String getWeakHash() { return weakHash; }
    public void updateWeakHash() { this.weakHash = calculateWeakHash(); }

    public List<Long> getFingerprints() { return fingerprints; }
    public void setFingerprints(List<Long> fingerprints) { this.fingerprints = fingerprints; }
}
