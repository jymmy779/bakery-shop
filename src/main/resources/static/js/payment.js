(() => {
  'use strict';

  document.querySelectorAll('[data-bank-transfer]').forEach(panel => {
    const feedback = panel.querySelector('[data-copy-feedback]');
    const showFeedback = message => {
      if (!feedback) return;
      // Update the text node in place for compatibility with the shared icon observer.
      if (feedback.firstChild?.nodeType === Node.TEXT_NODE) feedback.firstChild.nodeValue = message;
      else feedback.appendChild(document.createTextNode(message));
    };
    panel.querySelectorAll('[data-copy-target]').forEach(button => {
      button.addEventListener('click', async () => {
        const input = document.getElementById(button.dataset.copyTarget);
        if (!input || !panel.contains(input)) return;
        button.disabled = true;
        try {
          if (!navigator.clipboard?.writeText) throw new Error('Clipboard unavailable');
          await navigator.clipboard.writeText(input.value);
          showFeedback(`Đã sao chép ${button.dataset.copyLabel}.`);
        } catch {
          input.focus();
          input.select();
          showFeedback('Nội dung đã được chọn. Bạn hãy dùng thao tác Sao chép trên thiết bị.');
        } finally {
          button.disabled = false;
        }
      });
    });

    const image = panel.querySelector('[data-transfer-qr]');
    const error = panel.querySelector('[data-qr-error]');
    if (image && error) {
      const showQrError = () => { image.hidden = true; error.hidden = false; };
      image.addEventListener('error', showQrError, { once: true });
      if (image.complete && !image.naturalWidth) showQrError();
    }

    const tracking = panel.querySelector('[data-payment-tracking]');
    if (!tracking) return;
    const checkButton = tracking.querySelector('[data-check-payment]');
    const paymentFeedback = tracking.querySelector('[data-payment-feedback]');
    const tell = message => {
      if (paymentFeedback.firstChild?.nodeType === Node.TEXT_NODE) paymentFeedback.firstChild.nodeValue = message;
      else paymentFeedback.appendChild(document.createTextNode(message));
    };
    let timer;
    let request;
    let stopped = false;
    let active = true;
    const schedule = () => {
      clearTimeout(timer);
      if (!stopped && active && !document.hidden) timer = setTimeout(() => check(false), 10000);
    };
    const check = async manual => {
      if (stopped || request || !active || document.hidden) return;
      clearTimeout(timer);
      request = new AbortController();
      const timeout = setTimeout(() => request?.abort(), 10000);
      checkButton.disabled = true;
      if (manual) tell('Đang kiểm tra thanh toán…');
      try {
        const response = await fetch(panel.dataset.paymentStatusUrl, {
          method: 'GET', credentials: 'same-origin', cache: 'no-store',
          headers: { Accept: 'application/json' }, signal: request.signal
        });
        if (response.redirected || response.status === 401 || response.status === 403) {
          stopped = true;
          tell('Phiên đăng nhập đã hết hoặc không còn quyền truy cập. Vui lòng tải lại trang để đăng nhập.');
          return;
        }
        if (response.status === 404) {
          stopped = true;
          tell('Không tìm thấy đơn hàng. Vui lòng tải lại danh sách đơn hàng.');
          return;
        }
        if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) {
          throw new Error('Status unavailable');
        }
        const status = await response.json();
        if (String(status.orderId) !== panel.dataset.orderId
            || !['PENDING', 'CONFIRMED', 'DELIVERING', 'DELIVERED', 'CANCELLED'].includes(status.orderStatus)
            || !['PENDING', 'PAID'].includes(status.paymentStatus)) throw new Error('Invalid status');
        if (!active) return;
        if (status.orderStatus !== 'PENDING' || status.paymentStatus === 'PAID') {
          stopped = true;
          tell(status.orderStatus === 'CANCELLED' ? 'Đơn hàng đã được hủy. Đang cập nhật…'
            : status.paymentStatus === 'PAID' ? 'Thanh toán thành công! Đang cập nhật đơn hàng…'
            : 'Trạng thái đơn hàng đã thay đổi. Đang cập nhật…');
          // Refresh badges, receipt, QR and history from the saved server state together.
          window.location.reload();
          return;
        }
        tell('Chưa có xác nhận nhận tiền từ cửa hàng. Trang vẫn đang tự kiểm tra.');
      } catch {
        if (active && !document.hidden) tell('Chưa kiểm tra được kết quả. Trang sẽ tự thử lại; bạn cũng có thể bấm Kiểm tra thanh toán.');
      } finally {
        clearTimeout(timeout);
        request = null;
        checkButton.disabled = stopped;
        schedule();
      }
    };
    checkButton.addEventListener('click', () => check(true));
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) clearTimeout(timer);
      else check(false);
    });
    window.addEventListener('focus', () => check(false));
    window.addEventListener('pagehide', () => {
      active = false;
      clearTimeout(timer);
      request?.abort();
    });
    window.addEventListener('pageshow', event => {
      if (event.persisted) { active = true; check(false); }
    });
    check(false);
  });
})();
