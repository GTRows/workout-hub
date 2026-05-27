# MCP and connectors

Codex exposes tools through active plugins/connectors and the MCP servers configured by the Codex app. Do not run external MCP CLI commands for this project.

Use the active tool list first. When a user explicitly asks for a known installable connector that is not active, request it through the Codex connector install flow. For app-provided resources, prefer `tool_search` for deferred tool discovery and MCP resources only when they are already available in the session.
