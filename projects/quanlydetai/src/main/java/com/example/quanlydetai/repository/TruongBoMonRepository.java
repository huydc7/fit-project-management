package com.example.quanlydetai.repository;

import com.example.quanlydetai.entity.TruongBoMon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TruongBoMonRepository extends JpaRepository<TruongBoMon, String> {
    @Query(
            "select h from TruongBoMon h, GiangVien g where h.maGV=:teacher and h.dangGiuChuc=true"
                    + " and g.maGV=h.maGV and g.maBoMon=h.maBoMon")
    Optional<TruongBoMon> active(String teacher);
}
