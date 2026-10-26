package com.example.quanlydetai.controller;

import static com.example.quanlydetai.controller.support.FormFeedback.*;
import static com.example.quanlydetai.controller.support.PageResponses.add;

import com.example.quanlydetai.dto.DeTaiRequest;
import com.example.quanlydetai.dto.TopicSearch;
import com.example.quanlydetai.service.DeTaiService;
import com.example.quanlydetai.service.NhomService;
import com.example.quanlydetai.service.HoiDongService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class DeTaiController {
    private final DeTaiService topics;
    private final NhomService groups;
    private final HoiDongService councils;

    public DeTaiController(DeTaiService topics, NhomService groups, HoiDongService councils) {
        this.topics = topics;
        this.groups = groups;
        this.councils = councils;
    }

    @GetMapping("/lecturer/topics")
    public String teacherTopics(Model m) {
        if (!m.containsAttribute("form"))
            m.addAttribute("form", new DeTaiRequest(null, null, null, null, null));
        options(m);
        return "gv/topics";
    }

    @GetMapping("/lecturer/topics/{id}/edit")
    public String editTopic(@PathVariable String id, Model m) {
        var topic = topics.editableProposal(id);
        options(m);
        m.addAttribute("topic", topic);
        m.addAttribute("form", new com.example.quanlydetai.dto.NoiDungDeTaiRequest(
                topic.title(), topic.content(), topic.departmentId(), null));
        return "gv/topic-edit";
    }

    @PostMapping("/lecturer/topics")
    public String createTopic(
            @Valid @ModelAttribute("form") DeTaiRequest f,
            BindingResult errors,
            Model m,
            RedirectAttributes flash) {
        if (errors.hasErrors()) return teacherTopics(m);
        return formAction(
                m, flash, "/lecturer/topics", () -> topics.create(f), () -> teacherTopics(m));
    }

    @GetMapping("/dean/topics")
    public String facultyTopicManagement(Model m) {
        options(m);
        var page = topics.all((TopicSearch) m.getAttribute("topicSearch"),
                (Pageable) m.getAttribute("pageRequest"));
        add(m, "topics", page);
        m.addAttribute("teachers", topics.teachers());
        m.addAttribute("councilsByTopic", councils.optionsForTopics(page.stream()
                .filter(t -> "DangThucHien".equals(t.state())).map(t -> t.id()).toList()));
        m.addAttribute("managementScope", "Toàn bộ đề tài trong khoa");
        m.addAttribute("managementAction", "/dean/topics");
        return "topics/management";
    }

    @GetMapping("/department/topic-management")
    public String departmentTopicManagement(Model m) {
        options(m);
        add(m, "topics", topics.departmentTopics((TopicSearch) m.getAttribute("topicSearch"),
                (Pageable) m.getAttribute("pageRequest")));
        m.addAttribute("managementScope", "Đề tài thuộc bộ môn bạn phụ trách");
        m.addAttribute("managementAction", "/department/topic-management");
        return "topics/management";
    }

    @GetMapping("/lecturer/topic-management")
    public String teacherTopicManagement(Model m) {
        options(m);
        add(m, "topics", topics.mine((TopicSearch) m.getAttribute("topicSearch"),
                (Pageable) m.getAttribute("pageRequest")));
        m.addAttribute("managementScope", "Đề tài bạn đề xuất hoặc hướng dẫn");
        m.addAttribute("managementAction", "/lecturer/topic-management");
        return "topics/management";
    }

    @PostMapping("/dean/topics/{id}/review")
    public String reviewTopic(
            @PathVariable String id, @RequestParam boolean accept, RedirectAttributes flash) {
        return perform("/dean/approvals", flash, () -> topics.review(id, accept));
    }

    private void options(Model m) {
        m.addAttribute("periods", topics.periods());
        m.addAttribute("departments", topics.departments());
    }

    @GetMapping({"/student/topics", "/topics"})
    public String danhSach(Model model) {
        options(model);
        var topicPage =
                topics.available(
                        (TopicSearch) model.getAttribute("topicSearch"),
                        (Pageable) model.getAttribute("pageRequest"));
        add(model, "topics", topicPage);
        model.addAttribute(
                "groups",
                groups.registrationOptions(
                        topicPage.stream().map(t -> t.periodId()).distinct().toList()));
        return "sv/de-tai-list";
    }

    @ModelAttribute
    void filters(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String department,
            @RequestParam(defaultValue = "") String period,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String state,
            Model m) {
        m.addAttribute("topicSearch", new TopicSearch(q, department, period, type, state));
        m.addAttribute("q", q);
        m.addAttribute("departmentFilter", department);
        m.addAttribute("periodFilter", period);
        m.addAttribute("roleFilter", role);
        m.addAttribute("typeFilter", type);
        m.addAttribute("stateFilter", state);
    }

    @PostMapping("/lecturer/topics/{id}/edit")
    public String edit(
            @PathVariable String id,
            @Valid @ModelAttribute com.example.quanlydetai.dto.NoiDungDeTaiRequest f,
            BindingResult errors,
            Model m,
            RedirectAttributes flash) {
        String target = (String) m.getAttribute("topicManagementPath");
        if (errors.hasErrors()) {
            flash.addFlashAttribute("loi", "Tên/nội dung đề tài chưa hợp lệ.");
            return "redirect:" + target;
        }
        return perform(target, flash, () -> topics.updateContent(id, f));
    }

    @PostMapping("/lecturer/topics/{id}/delete")
    public String delete(@PathVariable String id, Model m, RedirectAttributes flash) {
        return perform((String) m.getAttribute("topicManagementPath"), flash, () -> topics.deleteDraft(id));
    }
}
