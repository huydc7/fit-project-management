package com.example.quanlydetai.controller.support;

import com.example.quanlydetai.exception.BusinessException;
import com.example.quanlydetai.exception.DatabaseErrorMessages;

import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


public final class FormFeedback {
    private FormFeedback() {}

    public static String perform(String target, RedirectAttributes flash, Runnable action) {
        try {
            action.run();
            flash.addFlashAttribute("thanhCong", "Đã lưu thành công.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("loi", ex.getMessage());
        } catch (DataAccessException ex) {
            flash.addFlashAttribute("loi", DatabaseErrorMessages.extractRootCauseMessage(ex));
        }
        return "redirect:" + target;
    }

    public static String formAction(
            Model m,
            RedirectAttributes flash,
            String target,
            Runnable action,
            java.util.function.Supplier<String> render) {
        try {
            action.run();
            flash.addFlashAttribute("thanhCong", "Đã lưu thành công.");
            return "redirect:" + target;
        } catch (BusinessException ex) {
            m.addAttribute("loi", ex.getMessage());
            return render.get();
        } catch (DataAccessException ex) {
            m.addAttribute("loi", DatabaseErrorMessages.extractRootCauseMessage(ex));
            return render.get();
        }
    }
}
