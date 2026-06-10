const menuToggle = document.querySelector(".menu-toggle");
const navigation = document.querySelector(".site-nav");

menuToggle.addEventListener("click", () => {
  const isOpen = navigation.classList.toggle("open");
  menuToggle.setAttribute("aria-expanded", String(isOpen));
});

navigation.querySelectorAll("a").forEach((link) => {
  link.addEventListener("click", () => {
    navigation.classList.remove("open");
    menuToggle.setAttribute("aria-expanded", "false");
  });
});

const revealObserver = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add("visible");
        revealObserver.unobserve(entry.target);
      }
    });
  },
  { threshold: 0.12 },
);

document.querySelectorAll(".reveal").forEach((element) => {
  revealObserver.observe(element);
});

document.querySelector("#year").textContent = new Date().getFullYear();

const assistantLauncher = document.querySelector(".assistant-launcher");
const assistantPanel = document.querySelector(".assistant-panel");
const assistantClose = document.querySelector(".assistant-close");
const assistantForm = document.querySelector(".assistant-form");
const assistantInput = document.querySelector("#assistant-input");
const assistantSubmit = assistantForm.querySelector("button");
const assistantMessages = document.querySelector(".assistant-messages");
const assistantSuggestions = document.querySelectorAll(".assistant-suggestions button");
const apiBaseUrl = (window.PORTFOLIO_API_BASE_URL || "").replace(/\/$/, "");
const sessionStorageKey = "portfolio-ai-session-id";

function getSessionId() {
  let sessionId = window.sessionStorage.getItem(sessionStorageKey);
  if (!sessionId) {
    sessionId = window.crypto.randomUUID();
    window.sessionStorage.setItem(sessionStorageKey, sessionId);
  }
  return sessionId;
}

function toggleAssistant(isOpen) {
  assistantPanel.classList.toggle("open", isOpen);
  assistantPanel.setAttribute("aria-hidden", String(!isOpen));
  assistantLauncher.setAttribute("aria-expanded", String(isOpen));
  if (isOpen) {
    assistantInput.focus();
  }
}

function addMessage(content, type) {
  document.querySelector(".assistant-empty")?.remove();
  const message = document.createElement("p");
  message.className = `assistant-message ${type}`;
  message.textContent = content;
  assistantMessages.append(message);
  assistantMessages.scrollTop = assistantMessages.scrollHeight;
  return message;
}

function setAssistantBusy(isBusy) {
  assistantInput.disabled = isBusy;
  assistantSubmit.disabled = isBusy;
  assistantSubmit.textContent = isBusy ? "生成中" : "发送";
}

function highlightElement(element) {
  if (!element) {
    return;
  }
  element.scrollIntoView({ behavior: "smooth", block: "center" });
  element.classList.add("assistant-highlight");
  window.setTimeout(() => element.classList.remove("assistant-highlight"), 3200);
}

function handleUiAction(action) {
  const allowedSections = new Set(["about", "projects", "experience", "contact"]);
  const allowedProjects = new Set([
    "driver-task-platform",
    "driver-campaign-platform",
    "openapi-service",
    "lbs-service",
    "grid-hot-service",
    "member-service",
    "third-party-service",
  ]);

  if (action.type === "open_contact") {
    highlightElement(document.querySelector("#contact"));
  }
  if (action.type === "scroll_to_section" && allowedSections.has(action.payload?.sectionId)) {
    highlightElement(document.querySelector(`#${action.payload.sectionId}`));
  }
  if (action.type === "highlight_project" && allowedProjects.has(action.payload?.projectId)) {
    highlightElement(document.querySelector(`[data-project-id="${action.payload.projectId}"]`));
  }
}

function parseSseEvent(rawEvent) {
  const lines = rawEvent.split("\n");
  const eventName = lines.find((line) => line.startsWith("event:"))?.slice(6).trim();
  const data = lines
    .filter((line) => line.startsWith("data:"))
    .map((line) => line.slice(5).trim())
    .join("\n");
  return { eventName, data: data ? JSON.parse(data) : {} };
}

async function sendQuestion(question) {
  const trimmedQuestion = question.trim();
  if (!trimmedQuestion) {
    return;
  }

  addMessage(trimmedQuestion, "user");
  const answer = addMessage("", "assistant");
  setAssistantBusy(true);

  try {
    const response = await fetch(`${apiBaseUrl}/api/v1/chat/stream`, {
      method: "POST",
      headers: {
        Accept: "text/event-stream",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        sessionId: getSessionId(),
        message: trimmedQuestion,
      }),
    });

    if (!response.ok || !response.body) {
      throw new Error("CHAT_REQUEST_FAILED");
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";

    while (true) {
      const { done, value } = await reader.read();
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done }).replace(/\r\n/g, "\n");
      const events = buffer.split("\n\n");
      buffer = events.pop() || "";

      events.forEach((rawEvent) => {
        if (!rawEvent.trim()) {
          return;
        }
        const event = parseSseEvent(rawEvent);
        if (event.eventName === "message_delta") {
          answer.textContent += event.data.content;
          assistantMessages.scrollTop = assistantMessages.scrollHeight;
        }
        if (event.eventName === "ui_action") {
          handleUiAction(event.data);
        }
        if (event.eventName === "error") {
          answer.classList.add("error");
          answer.textContent = event.data.message;
        }
      });

      if (done) {
        break;
      }
    }
  } catch (error) {
    answer.classList.add("error");
    answer.textContent = "AI 助手暂时不可用。你仍然可以浏览项目，或通过页面底部联系本人。";
  } finally {
    setAssistantBusy(false);
    assistantInput.focus();
  }
}

assistantLauncher.addEventListener("click", () => {
  toggleAssistant(!assistantPanel.classList.contains("open"));
});

assistantClose.addEventListener("click", () => toggleAssistant(false));

assistantSuggestions.forEach((button) => {
  button.addEventListener("click", () => sendQuestion(button.textContent));
});

assistantForm.addEventListener("submit", (event) => {
  event.preventDefault();
  const question = assistantInput.value;
  assistantInput.value = "";
  sendQuestion(question);
});

assistantInput.addEventListener("keydown", (event) => {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault();
    assistantForm.requestSubmit();
  }
});
