let authenticatedInThisPage = false;

export const hasAuthenticatedSession = (): boolean => authenticatedInThisPage;

export const markAuthenticatedSession = (): void => {
  authenticatedInThisPage = true;
};

export const clearAuthenticatedSession = (): void => {
  authenticatedInThisPage = false;
};
