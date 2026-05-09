// Minimal service worker (scope: /). Precaches the offline shell on
// install and serves /offline for any navigation that fails while the
// network is down. API requests are never cached. Handles push events
// from the WebPushNotificationDispatcher payload {title, body, url}
// and routes notification clicks to the embedded url. The cache key
// is versioned (wh-shell-vN); bumping it drops the previous cache on
// activate.
const CACHE = "wh-shell-v3";
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

self.addEventListener("push", (event) => {
  let payload = { title: "WorkoutHub", body: "", url: "/dashboard" };
  if (event.data) {
    try {
      payload = { ...payload, ...event.data.json() };
    } catch {
      payload.body = event.data.text();
    }
  }
  const opts = {
    body: payload.body,
    icon: "/icons/icon-192.png",
    badge: "/icons/icon-192.png",
    data: { url: payload.url || "/dashboard" },
    tag: "wh-reminder",
    renotify: false,
  };
  event.waitUntil(self.registration.showNotification(payload.title, opts));
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const target = (event.notification.data && event.notification.data.url) || "/dashboard";
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((all) => {
      for (const c of all) {
        try {
          const u = new URL(c.url);
          if (u.origin === self.location.origin) {
            c.focus();
            c.navigate(target).catch(() => {});
            return;
          }
        } catch {
          /* ignore non-URL clients */
        }
      }
      return self.clients.openWindow(target);
    })
  );
});
