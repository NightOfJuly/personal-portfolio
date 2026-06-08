// Set this to the deployed API origin before publishing the static site.
// GitHub Pages is served over HTTPS, so the production API origin must also be HTTPS.
window.PORTFOLIO_API_BASE_URL =
  window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1"
    ? "http://localhost:8080"
    : "https://grootq.top";
