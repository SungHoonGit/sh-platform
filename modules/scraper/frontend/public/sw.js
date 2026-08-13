// /sw.js - Web Push Service Worker
self.addEventListener('push', function(event) {
  console.log('[SW] Push received:', event.data ? event.data.text() : 'no data');
  if (!event.data) return;

  let data;
  try {
    data = event.data.json();
  } catch (e) {
    console.error('[SW] Failed to parse push data:', e);
    data = { title: 'SH Platform', body: event.data.text() };
  }
  
  const options = {
    body: data.body || '',
    icon: '/scraper/favicon.ico',
    badge: '/scraper/favicon.ico',
    vibrate: [100, 50, 100],
    data: {
      url: data.url || '/scraper/viewer'
    },
    actions: [
      { action: 'open', title: '열기' },
      { action: 'close', title: '닫기' }
    ]
  };

  event.waitUntil(
    self.registration.showNotification(data.title || 'SH Platform', options)
      .then(() => console.log('[SW] Notification shown'))
      .catch(e => console.error('[SW] Failed to show notification:', e))
  );
});

self.addEventListener('notificationclick', function(event) {
  event.notification.close();

  if (event.action === 'close') return;

  const urlToOpen = event.notification.data.url || '/scraper/viewer';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then(function(clientList) {
        for (const client of clientList) {
          if (client.url.includes('scraper') && 'focus' in client) {
            return client.focus();
          }
        }
        return clients.openWindow(urlToOpen);
      })
  );
});
