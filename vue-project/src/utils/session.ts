const CHAT_SESSION_ID_KEY = 'qau-smartqa-session-id';

const createSessionId = () => {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID();
  }

  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
};

export const getClientSessionId = () => {
  const cachedSessionId = localStorage.getItem(CHAT_SESSION_ID_KEY);

  if (cachedSessionId) {
    return cachedSessionId;
  }

  const sessionId = createSessionId();
  localStorage.setItem(CHAT_SESSION_ID_KEY, sessionId);
  return sessionId;
};
