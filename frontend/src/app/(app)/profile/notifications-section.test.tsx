import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/lib/api/client", () => ({
  api: {
    request: vi.fn(),
  },
}));

vi.mock("@/lib/push/subscribe", () => ({
  subscribePush: vi.fn(async () => undefined),
}));

import { NotificationsSection } from "./notifications-section";
import { api } from "@/lib/api/client";

const messages = {
  profile: {
    notifications: {
      title: "Notifications",
      description: "Push reminders for workouts, weight, and supplements on this device.",
      statusGranted: "Active on this device",
      statusDefault: "Permission needed",
      statusDenied: "Permission denied - clear browser site data to retry",
      statusUnsupported: "Not supported on this browser",
      disable: "Disable",
      testButton: "Send test notification",
    },
  },
  push: {
    test: {
      success: "Test notification sent (recipients: {count})",
      zero: "No subscriptions yet - nothing to send",
      error: "Failed to send test notification",
    },
  },
};

function renderSection() {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      <NotificationsSection />
    </NextIntlClientProvider>
  );
}

type SubscriptionStub = {
  endpoint: string;
  unsubscribe: () => Promise<boolean>;
};

function setPermission(value: NotificationPermission | undefined) {
  if (value === undefined) {
    Object.defineProperty(window, "Notification", {
      configurable: true,
      value: undefined,
    });
    return;
  }
  const resolved: NotificationPermission = value;
  Object.defineProperty(window, "Notification", {
    configurable: true,
    value: class MockNotification {
      static permission: NotificationPermission = resolved;
      static requestPermission = vi.fn(async () => "granted" as const);
    },
  });
}

function stubServiceWorker(subscription: SubscriptionStub | null) {
  Object.defineProperty(navigator, "serviceWorker", {
    configurable: true,
    value: {
      ready: Promise.resolve({
        pushManager: {
          getSubscription: async () => subscription,
        },
      }),
    },
  });
}

describe("NotificationsSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders the 'permission needed' state when permission is default", async () => {
    setPermission("default");
    stubServiceWorker(null);
    renderSection();

    await waitFor(() => {
      expect(screen.getByTestId("notifications-status")).toHaveTextContent(
        "Permission needed"
      );
    });
    expect(
      screen.queryByRole("button", { name: "Disable" })
    ).not.toBeInTheDocument();
  });

  it("renders Disable + Send test buttons when permission is granted and subscribed", async () => {
    setPermission("granted");
    const unsubscribe = vi.fn(async () => true);
    stubServiceWorker({
      endpoint: "https://push.example/abc",
      unsubscribe,
    });
    renderSection();

    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: "Disable" })
      ).toBeInTheDocument();
    });
    expect(
      screen.getByRole("button", { name: "Send test notification" })
    ).toBeInTheDocument();
  });

  it("calls subscription.unsubscribe and DELETE /api/push/subscribe on Disable click", async () => {
    setPermission("granted");
    const unsubscribe = vi.fn(async () => true);
    stubServiceWorker({
      endpoint: "https://push.example/del",
      unsubscribe,
    });
    (api.request as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);

    renderSection();
    await waitFor(() =>
      screen.getByRole("button", { name: "Disable" })
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Disable" }));

    await waitFor(() => expect(unsubscribe).toHaveBeenCalledTimes(1));
    expect(api.request).toHaveBeenCalledWith(
      expect.objectContaining({
        method: "DELETE",
        path: expect.stringContaining("/api/push/subscribe?endpoint="),
      })
    );
  });

  it("POSTs /api/push/test and shows the success toast on Send test click", async () => {
    setPermission("granted");
    const unsubscribe = vi.fn(async () => true);
    stubServiceWorker({
      endpoint: "https://push.example/ok",
      unsubscribe,
    });
    (api.request as ReturnType<typeof vi.fn>).mockResolvedValue({
      delivered: 2,
      subscriptions: 2,
    });

    renderSection();
    await waitFor(() =>
      screen.getByRole("button", { name: "Send test notification" })
    );

    const user = userEvent.setup();
    await user.click(
      screen.getByRole("button", { name: "Send test notification" })
    );

    await waitFor(() =>
      expect(api.request).toHaveBeenCalledWith(
        expect.objectContaining({
          method: "POST",
          path: "/api/push/test",
        })
      )
    );
    await waitFor(() =>
      expect(screen.getByTestId("pr-toast")).toHaveTextContent(
        /Test notification sent \(recipients: 2\)/
      )
    );
  });

  it("renders the denied state with no actions when permission is denied", async () => {
    setPermission("denied");
    stubServiceWorker(null);
    renderSection();

    await waitFor(() => {
      expect(screen.getByTestId("notifications-status")).toHaveTextContent(
        /Permission denied/
      );
    });
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
});
