package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.LuaChonResponse;
import com.example.quanlydetai.entity.BoMon;
import com.example.quanlydetai.entity.DotDangKy;
import com.example.quanlydetai.entity.GiangVien;
import com.example.quanlydetai.repository.JpaEntityRepository;

import jakarta.persistence.*;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ReferenceDataService {
    private final JpaEntityRepository entities;

    public ReferenceDataService(JpaEntityRepository entities) {
        this.entities = entities;
    }

    public List<LuaChonResponse> periods() {
        return entities
                .query("select d from DotDangKy d order by d.beginSV desc", DotDangKy.class)
                .stream()
                .map(
                        d ->
                                new LuaChonResponse(
                                        d.getMaDot(),
                                        d.getTenDotDangKy() + " (" + d.getLoai() + ")"))
                .toList();
    }

    public List<LuaChonResponse> departments() {
        return entities.query("select d from BoMon d order by d.tenBoMon", BoMon.class).stream()
                .map(d -> new LuaChonResponse(d.getMaBoMon(), d.getTenBoMon()))
                .toList();
    }

    public List<LuaChonResponse> searchDepartments(String q) {
        return departments().stream()
                .filter(d -> SearchText.matches(q, d.id(), d.label()))
                .toList();
    }

    public List<LuaChonResponse> teachers() {
        return entities
                .query("select g from GiangVien g order by g.hoTen", GiangVien.class)
                .stream()
                .map(g -> new LuaChonResponse(g.getMaGV(), g.getHoTen() + " — " + g.getMaGV()))
                .toList();
    }
}
