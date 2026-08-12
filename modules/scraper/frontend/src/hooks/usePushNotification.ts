import { useState, useEffect, useCallback } from "react";

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

export function usePushNotification() {
  const [isSupported, setIsSupported] = useState(false);
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [vapidKey, setVapidKey] = useState<string>("");

  useEffect(() => {
    setIsSupported("serviceWorker" in navigator && "PushManager" in window);
  }, []);

  useEffect(() => {
    if (!isSupported) return;
    checkSubscription();
    fetchVapidKey();
  }, [isSupported]);

  const fetchVapidKey = async () => {
    try {
      const res = await fetch("/scraper/api/v1/push/vapid-public-key");
      const data = await res.json();
      console.log("[PUSH] VAPID key fetched:", data.publicKey ? "OK" : "EMPTY");
      setVapidKey(data.publicKey || "");
    } catch (e) {
      console.error("[PUSH] Failed to fetch VAPID key:", e);
    }
  };

  const checkSubscription = async () => {
    try {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      setIsSubscribed(!!subscription);
    } catch {
      setIsSubscribed(false);
    }
  };

  const subscribe = useCallback(async () => {
    console.log("[PUSH] Subscribe called, isSupported:", isSupported, "vapidKey:", vapidKey ? "present" : "EMPTY");
    if (!isSupported || !vapidKey) return false;
    setLoading(true);

    try {
      const registration = await navigator.serviceWorker.ready;
      console.log("[PUSH] SW ready, subscribing with key length:", vapidKey.length);
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidKey) as BufferSource,
      });
      console.log("[PUSH] Subscription OK");

      const { endpoint } = subscription;
      const keys = subscription.toJSON().keys;

      const res = await fetch("/scraper/api/v1/push/subscribe", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          endpoint,
          p256dh: keys?.p256dh || "",
          auth: keys?.auth || "",
          userAgent: navigator.userAgent,
        }),
      });
      console.log("[PUSH] Server response:", res.status);

      setIsSubscribed(true);
      setLoading(false);
      return true;
    } catch (e) {
      console.error("[PUSH] Subscribe failed:", e);
      setLoading(false);
      return false;
    }
  }, [isSupported, vapidKey]);

  const unsubscribe = useCallback(async () => {
    setLoading(true);
    try {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      if (subscription) {
        await fetch("/scraper/api/v1/push/unsubscribe", {
          method: "DELETE",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ endpoint: subscription.endpoint }),
        });
        await subscription.unsubscribe();
      }
      setIsSubscribed(false);
      setLoading(false);
      return true;
    } catch (e) {
      console.error("Push unsubscribe failed:", e);
      setLoading(false);
      return false;
    }
  }, []);

  return { isSupported, isSubscribed, loading, subscribe, unsubscribe };
}
