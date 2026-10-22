package com.example.quanlydetai.repository;

import com.example.quanlydetai.dto.BaoCaoResponse;
import com.example.quanlydetai.entity.BaoCao;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BaoCaoRepository extends JpaRepository<BaoCao, String> {
    String SELECT_REPORT =
            "select new com.example.quanlydetai.dto.BaoCaoResponse(b.maBc, b.maNhom, n.tenNhom,"
                    + " t.maDeTai, t.tenDeTai, b.lanNop,"
                    + " cast(function('regexp_replace',b.fileDinhKem,'^.*[.]','') as String),"
                    + " b.ngayNop)";
    String MINE =
            " from BaoCao b join b.nhom n join n.deTai t where exists (select m.maSV from"
                    + " ThanhVienNhom m where m.maNhom=b.maNhom and m.maSV=:student)";
    String ASSIGNED =
            " from BaoCao b join b.nhom n join n.deTai t where (:dean=true or exists (select h.maGV"
                + " from GvhdDeTai h where h.maDeTai=t.maDeTai and h.maGV=:teacher) or exists"
                + " (select r.maGV from GvpbDeTai r where r.maDeTai=t.maDeTai and r.maGV=:teacher)"
                + " or exists (select m.maGV from ThanhVienHoiDong m where m.maGV=:teacher and"
                + " m.maHd in (select h.maHd from HoiDongDeTai h where h.maDeTai=t.maDeTai)))";

    @Query(
            value = SELECT_REPORT + ASSIGNED + " order by b.ngayNop desc, b.maBc",
            countQuery = "select count(b)" + ASSIGNED)
    Page<BaoCaoResponse> assigned(
            @Param("teacher") String teacher, @Param("dean") boolean dean, Pageable page);

    @Query(
            value = SELECT_REPORT + MINE + " order by b.ngayNop desc, b.maBc",
            countQuery = "select count(b)" + MINE)
    Page<BaoCaoResponse> mine(@Param("student") String student, Pageable page);

    @Query("select coalesce(max(b.lanNop),0) from BaoCao b where b.maNhom=:group")
    int lastVersion(@Param("group") String group);

    boolean existsByNhom_MaDeTai(String topic);

    @Query(
            SELECT_REPORT
                    + " from BaoCao b join b.nhom n join n.deTai t where t.maDeTai in :topics and"
                    + " b.lanNop=(select max(v.lanNop) from BaoCao v where v.maNhom=b.maNhom) order"
                    + " by b.maNhom")
    List<BaoCaoResponse> latestForTopics(@Param("topics") List<String> topics);
}
