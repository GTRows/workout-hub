import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { PushPermissionCard } from "@/components/push-permission-card";

const messages = {
  push: {
    title: "Notification permission",
    description: "Allow",
    enable: "Allow",
    pending: "Requesting...",
  },
};

function renderCard() {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      <PushPermissionCard />
    </NextIntlClientProvider>
  );
}

function setPermission(value: NotificationPermission) {
  Object.defineProperty(window.Notification, "permission", {
    configurable: true,
    get: () => value,
  });
}

describe("PushPermissionCard", () => {
  beforeEach(() => {
    Object.defineProperty(window, "Notification", {
      configurable: true,
      value: class MockNotification {
        static permission: NotificationPermission = "default";
        static requestPermission = vi.fn(async () => "granted" as const);
      },
    });
    Object.defineProperty(navigator, "serviceWorker", {
      configurable: true,
      value: { register: vi.fn(), ready: Promise.resolve({}) },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders the enable button when permission is default", () => {
    setPermission("default");
    renderCard();
    expect(screen.getByTestId("push-permission-card")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Allow" })).toBeInTheDocument();
  });

  it("does not render when permission has already been granted", () => {
    setPermission("granted");
    renderCard();
    expect(screen.queryByTestId("push-permission-card")).not.toBeInTheDocument();
  });

  it("does not render when permission has been denied", () => {
    setPermission("denied");
    renderCard();
    expect(screen.queryByTestId("push-permission-card")).not.toBeInTheDocument();
  });

  it("does not render when the browser does not support notifications", () => {
    Object.defineProperty(window, "Notification", {
      configurable: true,
      value: undefined,
    });
    renderCard();
    expect(screen.queryByTestId("push-permission-card")).not.toBeInTheDocument();
  });
});
