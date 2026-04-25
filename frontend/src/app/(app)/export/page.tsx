import { ExportClient } from "./export-client";
import { HealthImportClient } from "./health-import-client";

export default function Page() {
  return (
    <div className="space-y-4">
      <ExportClient />
      <HealthImportClient />
    </div>
  );
}
