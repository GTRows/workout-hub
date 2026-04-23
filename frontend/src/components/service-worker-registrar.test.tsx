import { render } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ServiceWorkerRegistrar } from "@/components/service-worker-registrar";

describe("ServiceWorkerRegistrar", () => {
  beforeEach(() => {
    Object.defineProperty(document, "readyState", {
      configurable: true,
      get: () => "complete",
    });
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.restoreAllMocks();
  });

  it("registers /sw.js when running in production with SW support", () => {
    vi.stubEnv("NODE_ENV", "production");
    const register = vi.fn(() => Promise.resolve({} as ServiceWorkerRegistration));
    Object.defineProperty(navigator, "serviceWorker", {
      configurable: true,
      value: { register },
    });

    render(<ServiceWorkerRegistrar />);

    expect(register).toHaveBeenCalledWith("/sw.js");
  });

  it("does nothing outside of production", () => {
    vi.stubEnv("NODE_ENV", "development");
    const register = vi.fn();
    Object.defineProperty(navigator, "serviceWorker", {
      configurable: true,
      value: { register },
    });

    render(<ServiceWorkerRegistrar />);

    expect(register).not.toHaveBeenCalled();
  });
});
