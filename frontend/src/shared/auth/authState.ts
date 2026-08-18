const AUTHENTICATED_MARKER = "nexo.authenticated";

const canUseStorage = (): boolean => typeof window !== "undefined" && typeof window.sessionStorage !== "undefined";

export const hasAuthenticatedSession = (): boolean => canUseStorage() && window.sessionStorage.getItem(AUTHENTICATED_MARKER) === "true";

export const markAuthenticatedSession = (): void => {
  if (canUseStorage()) window.sessionStorage.setItem(AUTHENTICATED_MARKER, "true");
};

export const clearAuthenticatedSession = (): void => {
  if (canUseStorage()) window.sessionStorage.removeItem(AUTHENTICATED_MARKER);
};
