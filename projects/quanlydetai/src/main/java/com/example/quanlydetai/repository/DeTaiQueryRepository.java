package com.example.quanlydetai.repository;

import com.example.quanlydetai.dto.TopicSearch;
import com.example.quanlydetai.entity.DeTai;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class DeTaiQueryRepository extends QuerySupport {
    public Page<DeTai> search(
            TopicSearch filter,
            String teacher,
            String managedDepartment,
            boolean publishedOnly,
            Pageable page) {
        return search(filter, teacher, managedDepartment, publishedOnly, false, page);
    }

    public Page<DeTai> pendingApproval(TopicSearch filter, Pageable page) {
        // Queue scope is fixed by the server, independently of request state filters.
        return search(new TopicSearch(filter.q(), filter.department(), filter.period(), filter.type(), ""),
                null, null, false, true, page);
    }

    private Page<DeTai> search(
            TopicSearch filter, String teacher, String managedDepartment,
            boolean publishedOnly, boolean approvalOnly, Pageable page) {
        var params = new HashMap<String, Object>();
        var from = new StringBuilder("from DeTai t join fetch t.dot d join fetch t.boMon where ");
        from.append(contains(filter.q(), params, "t.maDeTai", "t.tenDeTai", "t.noiDung"));
        if (teacher != null) {
            from.append(
                    " and (t.maGvDangKy=:teacher or exists (select h.maGV from GvhdDeTai h where"
                            + " h.maDeTai=t.maDeTai and h.maGV=:teacher))");
            params.put("teacher", teacher);
        }
        // The service supplies this scope; request filters can only narrow it.
        equal(from, params, "t.maBoMon", "managedDepartment", managedDepartment);
        if (publishedOnly) from.append(" and t.trangThai='DaCongBo'");
        if (approvalOnly) from.append(" and t.trangThai='Nhap' and exists"
                + " (select a.maDeTai from DuyetBoMon a where a.maDeTai=t.maDeTai)");
        equal(from, params, "t.maBoMon", "department", filter.department());
        equal(from, params, "t.maDot", "period", filter.period());
        equal(from, params, "d.loai", "type", filter.type());
        equal(from, params, "t.trangThai", "state", filter.state());
        return page(
                "select t",
                from.toString(),
                "t.ngayDangKy desc, t.maDeTai",
                "count(t)",
                DeTai.class,
                params,
                page);
    }
}
