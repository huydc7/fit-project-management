package com.example.quanlydetai.entity;

import java.io.Serializable;
import java.util.Objects;

public class GvpbDeTaiId implements Serializable {
    private static final long serialVersionUID = 1L;
    private String maGV;
    private String maDeTai;

    public GvpbDeTaiId() {}

    public GvpbDeTaiId(String maGV, String maDeTai) {
        this.maGV = maGV;
        this.maDeTai = maDeTai;
    }

    public String getMaGV() {
        return maGV;
    }

    public void setMaGV(String maGV) {
        this.maGV = maGV;
    }

    public String getMaDeTai() {
        return maDeTai;
    }

    public void setMaDeTai(String maDeTai) {
        this.maDeTai = maDeTai;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GvpbDeTaiId id)) return false;
        return Objects.equals(maGV, id.maGV) && Objects.equals(maDeTai, id.maDeTai);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maGV, maDeTai);
    }
}
