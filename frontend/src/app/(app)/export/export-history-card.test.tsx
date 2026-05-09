import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { ExportHistoryCard } from "./export-history-card";
import { appendHistoryEntry } from "./export-history";

const messages = {
  export: {
    historyTitle: "Download history",
    historyDescription: "Last 10 export actions on this device.",
    historyEmpty: "No previous exports yet.",
    historyClear: "Clear history",
    historyKind: {
      claude: "Claude summary",
      full: "Full JSON dump",
      section: "Section",
      csv: "CSV",
    },
  },
};

function renderCard(ui: ReactElement) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>,
  );
}

describe("ExportHistoryCard", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  afterEach(() => {
    window.localStorage.clear();
  });

  it("renders the empty state when history is empty", () => {
    renderCard(<ExportHistoryCard />);
    expect(screen.getByTestId("history-empty")).toHaveTextContent(
      "No previous exports yet.",
    );
    const clearBtn = screen.getByTestId("history-clear");
    expect(clearBtn).toBeDisabled();
  });

  it("renders the most recent entries with their kind label", () => {
    appendHistoryEntry({
      kind: "claude",
      filename: "a.json",
      timestamp: Date.now() - 60_000,
    });
    appendHistoryEntry({
      kind: "section",
      filename: "metrics.json",
      sectionLabel: "metrics",
      timestamp: Date.now(),
    });
    renderCard(<ExportHistoryCard />);
    const list = screen.getByTestId("history-list");
    expect(within(list).getAllByRole("listitem")).toHaveLength(2);
    expect(list).toHaveTextContent("Claude summary");
    expect(list).toHaveTextContent("Section: metrics");
    expect(list).toHaveTextContent("a.json");
    expect(list).toHaveTextContent("metrics.json");
  });

  it("clears the history when the clear button is clicked", async () => {
    appendHistoryEntry({ kind: "csv", filename: "x.csv" });
    renderCard(<ExportHistoryCard />);
    expect(screen.getByTestId("history-list")).toBeInTheDocument();
    const user = userEvent.setup();
    await user.click(screen.getByTestId("history-clear"));
    expect(await screen.findByTestId("history-empty")).toBeInTheDocument();
  });
});
