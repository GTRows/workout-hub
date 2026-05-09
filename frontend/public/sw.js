// Minimal service worker (scope: /). Precaches the offline shell on
// install and serves /offline for any navigation that fails while the
// network is down. API requests are never cached. The cache key is
// versioned (wh-shell-vN); bumping it drops the previous cache on
// activate.
const CACHE = "wh-shell-v2";
const SHELL = ["/offline", "/manifest.webmanifest", "/icons/icon-192.png"];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE).then((cache) => cache.addAll(SHELL)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))
      )
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;

  const url = new URL(req.url);
  if (url.pathname.startsWith("/api/")) return;

  if (req.mode === "navigate") {
    event.respondWith(
      fetch(req).catch(() =>
        caches.match("/offline").then((r) => r ?? new Response("offline", { status: 503 }))
      )
    );
    return;
  }

  event.respondWith(caches.match(req).then((cached) => cached ?? fetch(req)));
});
