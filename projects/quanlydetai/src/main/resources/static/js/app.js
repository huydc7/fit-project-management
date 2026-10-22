(() => {
  const toggle = document.querySelector('[data-menu-toggle]');
  const backdrop = document.querySelector('[data-menu-close]');
  const sidebar = document.getElementById('sidebar');
  const setMenu = (open) => {
    document.body.classList.toggle('menu-open', open);
    toggle?.setAttribute('aria-expanded', String(open));
    toggle?.setAttribute('aria-label', open ? 'Đóng menu điều hướng' : 'Mở menu điều hướng');
    if (backdrop) backdrop.hidden = !open;
  };
  if (toggle && sidebar) {
    document.body.classList.add('navigation-ready');
    toggle.addEventListener('click', () => setMenu(toggle.getAttribute('aria-expanded') !== 'true'));
    backdrop?.addEventListener('click', () => { setMenu(false); toggle.focus(); });
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape' && toggle.getAttribute('aria-expanded') === 'true') { setMenu(false); toggle.focus(); }
    });
    document.addEventListener('focusin', event => {
      if (toggle.getAttribute('aria-expanded') === 'true' && !sidebar.contains(event.target) && event.target !== toggle && event.target !== backdrop) setMenu(false);
    });
    window.matchMedia('(min-width: 1001px)').addEventListener('change', event => { if (event.matches) setMenu(false); });
  }
  document.querySelectorAll('[data-password-toggle]').forEach(button => {
    const field = document.getElementById(button.getAttribute('aria-controls'));
    if (!field) return;
    button.hidden = false;
    button.addEventListener('click', () => {
      const show = field.type === 'password';
      field.type = show ? 'text' : 'password';
      const label = show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu';
      button.setAttribute('aria-label', label);
      button.setAttribute('title', label);
      const eyeOpen = button.querySelector('[data-eye-open]');
      const eyeClosed = button.querySelector('[data-eye-closed]');
      if (eyeOpen) eyeOpen.toggleAttribute('hidden', show);
      if (eyeClosed) eyeClosed.toggleAttribute('hidden', !show);
      button.setAttribute('aria-pressed', String(show));
    });
  });
})();

document.querySelectorAll('form[data-confirm]').forEach(form => {
  form.addEventListener('submit', event => { if (!window.confirm(form.dataset.confirm)) event.preventDefault(); });
});

document.querySelectorAll('[data-teacher-picker]').forEach(picker => {
  const form = picker.closest('form');
  const choices = [...picker.querySelectorAll('input[type="checkbox"]')];
  const limit = Number(picker.dataset.max);
  const council = picker.dataset.kind === 'council';
  const roles = council ? [form.querySelector('#chair'), form.querySelector('#secretary')] : [];
  const count = picker.querySelector('[data-teacher-count]');
  const feedback = picker.querySelector('[data-teacher-feedback]');
  const submit = form.querySelector('[data-teacher-submit]');
  const updateSelection = () => {
    const reserved = new Map(roles.filter(field => field.value)
      .map(field => [field.value, field.id === 'chair' ? 'Chủ tịch' : 'Thư ký']));
    const duplicateRoles = council && roles[0].value && roles[0].value === roles[1].value;
    roles.forEach((field, index) => {
      const other = roles[1 - index];
      [...field.options].forEach(option => {
        option.disabled = !!option.value && option.value === other.value;
      });
    });
    if (council) roles[1].setCustomValidity(duplicateRoles ? 'Chủ tịch và Thư ký phải là hai người khác nhau.' : '');
    choices.forEach(choice => {
      const role = reserved.get(choice.value);
      if (role) choice.checked = false;
      const badge = choice.closest('.teacher-option').querySelector('[data-teacher-role]');
      if (badge) { badge.textContent = role || ''; badge.hidden = !role; }
    });
    const selected = choices.filter(choice => choice.checked).length;
    count.textContent = council
      ? `Thêm ${selected}/${limit} · Tổng ${reserved.size + selected}/5 người`
      : `Đã chọn ${selected}/${limit}`;
    choices.forEach(choice => {
      choice.disabled = reserved.has(choice.value) || (!choice.checked && selected >= limit);
    });
    const rolesReady = !council || (roles.every(field => field.value) && !duplicateRoles);
    submit.disabled = !rolesReady || selected < 1 || selected > limit;
    if (duplicateRoles) {
      feedback.textContent = 'Chủ tịch và Thư ký phải là hai người khác nhau.';
    } else if (!rolesReady) {
      feedback.textContent = 'Chọn Chủ tịch và Thư ký; sau đó chọn thêm 1–3 thành viên khác.';
    } else if (selected === 0) {
      feedback.textContent = council
        ? 'Chọn ít nhất 1 thành viên khác để hội đồng đủ 3 người.'
        : 'Chọn ít nhất 1 giảng viên để duyệt đề tài.';
    } else if (selected >= limit) {
      feedback.textContent = `Đã chọn đủ ${limit} giảng viên. Bỏ chọn một người nếu muốn thay đổi.`;
    } else {
      feedback.textContent = `Có thể chọn thêm ${limit - selected} ${council ? 'thành viên' : 'giảng viên hướng dẫn'}.`;
    }
    return !submit.disabled;
  };
  choices.forEach(choice => choice.addEventListener('change', updateSelection));
  roles.forEach(field => field.addEventListener('change', updateSelection));
  form.addEventListener('submit', event => {
    if (!updateSelection()) {
      event.preventDefault();
      choices.find(choice => !choice.disabled)?.focus();
    }
  });
  const search = picker.querySelector('[data-teacher-search]');
  const rows = [...picker.querySelectorAll('.teacher-option')];
  const normalize = value => value.toLocaleLowerCase('vi').normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').trim();
  picker.querySelector('[data-teacher-search-control]').hidden = false;
  search.addEventListener('keydown', event => {
    if (event.key === 'Enter') event.preventDefault();
  });
  search.addEventListener('input', () => {
    const query = normalize(search.value);
    rows.forEach(row => { row.hidden = !normalize(row.textContent).includes(query); });
    picker.querySelector('[data-teacher-no-results]').hidden = rows.length === 0
      || rows.some(row => !row.hidden);
  });
  updateSelection();
});
