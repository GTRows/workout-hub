import { act, render } from "@testing-library/react";
import { createElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const replaceMock = vi.fn();
let pathnameValue = "/dashboard";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: replaceMock, refresh: vi.fn() }),
  usePathname: () => pathnameValue,
}));

import {
  clearTokens,
  setAccessToken,
  setRefreshToken,
} from "@/lib/auth/token-store";
import { useSessionExpiredRedirect } from "@/lib/auth/use-session-expired-redirect";

function TestHook() {
  useSessionExpiredRedirect();
  return null;
}

describe("useSessionExpiredRedirect", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
    replaceMock.mockReset();
    pathnameValue = "/dashboard";
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("does not redirect when access token transitions null -> null", () => {
    render(createElement(TestHook));
    act(() => {
      setAccessToken(null);
    });
    expect(replaceMock).not.toHaveBeenCalled();
  });

  it("does not redirect when pathname starts with /login", () => {
    pathnameValue = "/login";
    setAccessToken("a");
    setRefreshToken("r");
    render(createElement(TestHook));
    act(() => {
      clearTokens();
    });
    expect(replaceMock).not.toHaveBeenCalled();
  });

  it("redirects to /login?reason=session-expired&next=<path> when access token clears on a protected path", () => {
    setAccessToken("a");
    setRefreshToken("r");
    render(createElement(TestHook));
    act(() => {
      clearTokens();
    });
    expect(replaceMock).toHaveBeenCalledWith(
      "/login?reason=session-expired&next=%2Fdashboard"
    );
  });
});
