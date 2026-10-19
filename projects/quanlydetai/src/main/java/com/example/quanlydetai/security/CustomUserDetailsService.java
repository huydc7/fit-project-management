package com.example.quanlydetai.security;

import com.example.quanlydetai.entity.TaiKhoan;
import com.example.quanlydetai.repository.TaiKhoanRepository;
import com.example.quanlydetai.repository.TruongKhoaRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {
    private final TaiKhoanRepository accounts;
    private final TruongKhoaRepository deans;
    private final com.example.quanlydetai.repository.TruongBoMonRepository heads;

    public CustomUserDetailsService(
            TaiKhoanRepository accounts,
            TruongKhoaRepository deans,
            com.example.quanlydetai.repository.TruongBoMonRepository heads) {
        this.accounts = accounts;
        this.deans = deans;
        this.heads = heads;
    }

    @Override
    public AccountPrincipal loadUserByUsername(String username) {
        return principal(accounts.findByTenDangNhap(username).orElseThrow(this::notFound));
    }

    public AccountPrincipal loadById(String id) {
        return principal(accounts.findById(id).orElseThrow(this::notFound));
    }

    private UsernameNotFoundException notFound() {
        return new UsernameNotFoundException("Tài khoản không hợp lệ.");
    }

    private AccountPrincipal principal(TaiKhoan account) {
        String role =
                switch (account.getVaiTro()) {
                    case "SV" -> "ROLE_SV";
                    case "GV" -> "ROLE_GV";
                    case "Admin" -> "ROLE_ADMIN";
                    default -> throw notFound();
                };
        var authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority(role));
        if ("GV".equals(account.getVaiTro())
                && account.getMaGV() != null
                && deans.findById(account.getMaGV()).map(d -> d.isDangGiuChuc()).orElse(false)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_TRUONGKHOA"));
        }
        if ("GV".equals(account.getVaiTro())
                && account.getMaGV() != null
                && heads.active(account.getMaGV()).isPresent())
            authorities.add(new SimpleGrantedAuthority("ROLE_TRUONGBOMON"));
        return new AccountPrincipal(
                account.getMaTk(),
                account.getTenDangNhap(),
                account.getMatKhauHash(),
                "HoatDong".equals(account.getTrangThai()),
                account.getMaGV(),
                account.getMaSV(),
                authorities);
    }
}
