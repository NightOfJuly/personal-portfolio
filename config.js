// Set this to the deployed API origin before publishing the static site.
// An empty value is useful when a reverse proxy serves the API on the same origin.
window.PORTFOLIO_API_BASE_URL =
  window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1"
    ? "http://localhost:8080"
    : "";
