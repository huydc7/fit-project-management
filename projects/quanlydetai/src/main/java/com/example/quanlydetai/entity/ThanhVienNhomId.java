package com.example.quanlydetai.entity;

import java.io.Serializable;
import java.util.Objects;

public class ThanhVienNhomId implements Serializable {
    private static final long serialVersionUID = 1L;
    private String maNhom;
    private String maSV;

    public ThanhVienNhomId() {}

    public ThanhVienNhomId(String maNhom, String maSV) {
        this.maNhom = maNhom;
        this.maSV = maSV;
    }

    public String getMaNhom() {
        return maNhom;
    }

    public void setMaNhom(String maNhom) {
        this.maNhom = maNhom;
    }

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String maSV) {
        this.maSV = maSV;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThanhVienNhomId id)) return false;
        return Objects.equals(maNhom, id.maNhom) && Objects.equals(maSV, id.maSV);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maNhom, maSV);
    }
}
