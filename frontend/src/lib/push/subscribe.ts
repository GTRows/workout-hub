import { api } from "@/lib/api/client";
import {
  pushSubscribeRequestSchema,
  vapidPublicKeyResponseSchema,
  type PushSubscribeRequest,
} from "@/lib/api/schemas";

function urlBase64ToArrayBuffer(base64String: string): ArrayBuffer {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const raw = atob(base64);
  const buf = new ArrayBuffer(raw.length);
  const view = new Uint8Array(buf);
  for (let i = 0; i < raw.length; i++) view[i] = raw.charCodeAt(i);
  return buf;
}

function bufferToBase64Url(buf: ArrayBuffer | null): string {
  if (!buf) return "";
  const bytes = new Uint8Array(buf);
  let bin = "";
  for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export async function subscribePush(): Promise<void> {
  if (typeof window === "undefined") return;
  if (!("serviceWorker" in navigator) || !("PushManager" in window)) {
    throw new Error("Push not supported");
  }

  const { publicKey } = await api.request({
    path: "/api/push/vapid-public-key",
    schema: vapidPublicKeyResponseSchema,
  });
  if (!publicKey) throw new Error("VAPID public key not configured");

  const reg = await navigator.serviceWorker.ready;
  const subscription = await reg.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToArrayBuffer(publicKey),
  });

  const body: PushSubscribeRequest = {
    endpoint: subscription.endpoint,
    keys: {
      p256dh: bufferToBase64Url(subscription.getKey("p256dh")),
      auth: bufferToBase64Url(subscription.getKey("auth")),
    },
    userAgent: navigator.userAgent,
  };

  await api.request({
    method: "POST",
    path: "/api/push/subscribe",
    body: pushSubscribeRequestSchema.parse(body),
  });
}
