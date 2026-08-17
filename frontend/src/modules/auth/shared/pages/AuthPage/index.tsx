import { useState, type ReactElement } from "react";
import { CreateOwnerForm } from "../../../bootstrap/components/CreateOwnerForm";
import { LoginForm } from "../../../session/components/LoginForm";
import { ForgotPasswordForm } from "../../../recovery/components/ForgotPasswordForm";
import { ResetPasswordForm } from "../../../recovery/components/ResetPasswordForm";
import type { AuthPageProps, AuthView } from "../../../types/authTypes";
import { Brand, BrandContent, BrandName, BrandPromise, Card, Logo, Page } from "./styles";

export function AuthPage({ bootstrapRequired }: AuthPageProps): ReactElement {
  const resetToken: string = new URLSearchParams(window.location.search).get("token") ?? "";
  const initialView: AuthView = resetToken ? "reset-password" : "login";
  const [view, setView] = useState<AuthView>(initialView);

  const showLogin = (): void => {
    window.history.replaceState({}, "", "/");
    setView("login");
  };

  const content: ReactElement = bootstrapRequired
    ? <CreateOwnerForm />
    : view === "forgot-password"
      ? <ForgotPasswordForm onBackToLogin={showLogin} />
      : view === "reset-password"
        ? <ResetPasswordForm token={resetToken} onResetCompleted={showLogin} />
        : <LoginForm onForgotPassword={(): void => setView("forgot-password")} />;

  return (
    <Page>
      <Brand aria-label="Nexo IA">
        <Logo src="/assets/logo/nexo-ia-symbol.png" alt="" />
        <BrandContent>
          <BrandName>Nexo IA</BrandName>
          <BrandPromise>Your knowledge. Your tools. Your control.</BrandPromise>
        </BrandContent>
      </Brand>
      <Card>{content}</Card>
    </Page>
  );
}
