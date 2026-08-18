import type { ReactElement } from "react";
import { AuthPage } from "../../modules/auth/shared/pages/AuthPage";
import { useAuthSession } from "../../modules/auth/shared/hooks/useAuthSession";
import type { AuthSessionResult } from "../../modules/auth/types/authTypes";
import { AppShell } from "../components/AppShell";
import { Snackbar } from "../../shared/feedback/components/Snackbar";
import { ConfirmationModal } from "../../shared/feedback/components/ConfirmationModal";
import { SessionExpiredModal } from "../../shared/feedback/components/SessionExpiredModal";
import { AppRoot, State } from "./styles";

export function App(): ReactElement {
  const auth: AuthSessionResult = useAuthSession();
  let content: ReactElement;

  if (auth.isLoading || auth.bootstrapRequired === undefined) {
    content = (
      <AppRoot>
        <State>Connecting to Nexo…</State>
      </AppRoot>
    );
  } else if (auth.error) {
    content = (
      <AppRoot>
        <State role="alert">Nexo is unavailable. Check the backend connection.</State>
      </AppRoot>
    );
  } else if (auth.bootstrapRequired || !auth.user) {
    content = <AuthPage bootstrapRequired={auth.bootstrapRequired} />;
  } else {
    content = (
      <AppRoot>
        <AppShell
          user={auth.user}
          onLogout={auth.logout}
          isLoggingOut={auth.isLoggingOut}
        />
      </AppRoot>
    );
  }

  return (
    <>
      {content}
      <Snackbar />
      <ConfirmationModal />
      <SessionExpiredModal />
    </>
  );
}
