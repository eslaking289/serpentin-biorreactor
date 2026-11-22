const CACHE_NAME = 'thermabio-v5';
const APP_FILES = [
  './', './index.html', './balance-metabolico.html', './balance-metabolico-core.js', './metodologia.html', './styles.css', './core.js', './app.js',
  './manifest.webmanifest'
];
const OPTIONAL_FILES = ['./icons/icon.svg', './icons/icon-192.png', './icons/icon-512.png'];

self.addEventListener('install', event => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE_NAME);
    await cache.addAll(APP_FILES);
    await Promise.all(OPTIONAL_FILES.map(async path => {
      const request = new Request(new URL(path, self.location.href), { cache: 'reload' });
      try {
        const response = await fetch(request);
        if (response.ok) await cache.put(request, response);
      } catch (_) {
        // Optional icons do not block installation of the offline app shell.
      }
    }));
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key)));
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  const url = new URL(event.request.url);
  if (url.origin !== self.location.origin) return;
  event.respondWith((async () => {
    try {
      const response = await fetch(event.request);
      if (response.ok) {
        try {
          const cache = await caches.open(CACHE_NAME);
          await cache.put(event.request, response.clone());
        } catch (_) {
          // A quota/storage error must not prevent the online response.
        }
        return response;
      }
      const cached = await caches.match(event.request);
      return cached || response;
    } catch (_) {
      const cached = await caches.match(event.request);
      if (cached) return cached;
      if (event.request.mode === 'navigate') {
        const appShell = new URL('./index.html', self.location.href);
        return (await caches.match(appShell)) || Response.error();
      }
      return Response.error();
    }
  })());
});
