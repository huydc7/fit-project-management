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
