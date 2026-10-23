package com.example.quanlydetai.service;

import com.example.quanlydetai.dto.LuaChonResponse;
import com.example.quanlydetai.entity.BoMon;
import com.example.quanlydetai.entity.DotDangKy;
import com.example.quanlydetai.entity.GiangVien;
import com.example.quanlydetai.repository.CatalogQueryRepository;
import com.example.quanlydetai.repository.JpaEntityRepository;


import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ReferenceDataService {
    private final JpaEntityRepository entities;

    private final CatalogQueryRepository queries;

    public ReferenceDataService(JpaEntityRepository entities, CatalogQueryRepository queries) {
        this.queries = queries;
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
        return searchDepartments(q, Paging.defaults()).getContent();
    }

    public Page<LuaChonResponse> searchDepartments(String q, Pageable page) {
        Paging.check(page);
        return queries.departments(q, page)
                .map(d -> new LuaChonResponse(d.getMaBoMon(), d.getTenBoMon()));
    }

    public List<LuaChonResponse> teachers() {
        return activeTeachers(null);
    }

    public List<LuaChonResponse> teachersInDepartment(String department) {
        return activeTeachers(department);
    }

    private List<LuaChonResponse> activeTeachers(String department) {
        String query =
                "select g from GiangVien g where exists (select a.maTk from TaiKhoan a"
                        + " where a.maGV=g.maGV and a.vaiTro='GV' and a.trangThai='HoatDong')";
        if (department != null) query += " and g.maBoMon=?1";
        return entities
                .query(
                        query + " order by g.hoTen",
                        GiangVien.class,
                        department == null ? new Object[0] : new Object[] {department})
                .stream()
                .map(g -> new LuaChonResponse(g.getMaGV(), g.getHoTen() + " — " + g.getMaGV()))
                .toList();
    }

    // Administration must still be able to revoke appointments of a locked lecturer.
    public List<LuaChonResponse> allTeachers() {
        return entities
                .query("select g from GiangVien g order by g.hoTen", GiangVien.class)
                .stream()
                .map(g -> new LuaChonResponse(g.getMaGV(), g.getHoTen() + " — " + g.getMaGV()))
                .toList();
    }
}
