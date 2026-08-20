import { Brain, FolderOpen, ShieldCheck } from "@phosphor-icons/react";
import { useState, type ReactElement } from "react";
import { CreateOwnerForm } from "../../../bootstrap/components/CreateOwnerForm";
import { LoginForm } from "../../../session/components/LoginForm";
import { ForgotPasswordForm } from "../../../recovery/components/ForgotPasswordForm";
import { ResetPasswordForm } from "../../../recovery/components/ResetPasswordForm";
import type { AuthPageProps, AuthView } from "../../../types/authTypes";
import { Brand, BrandContent, BrandName, BrandPromise, Card, Feature, Features, Logo, Page } from "./styles";

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
          <Features>
            <Feature><Brain size={20} weight="duotone" /><div><strong>Understand</strong><span>Talk to models and ground answers in your own context.</span></div></Feature>
            <Feature><FolderOpen size={20} weight="duotone" /><div><strong>Build</strong><span>Work with authorized projects through visible plans and diffs.</span></div></Feature>
            <Feature><ShieldCheck size={20} weight="duotone" /><div><strong>Stay in control</strong><span>Every capability remains scoped, inspectable and accountable.</span></div></Feature>
          </Features>
        </BrandContent>
      </Brand>
      <Card>{content}</Card>
    </Page>
  );
}
